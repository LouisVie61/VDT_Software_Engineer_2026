from __future__ import annotations

import argparse
import json
import math
from pathlib import Path
from statistics import median
from typing import Any


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Create a paired A/B comparison from two evaluation JSON reports.")
    parser.add_argument("--baseline", required=True, type=Path)
    parser.add_argument("--candidate", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    return parser.parse_args()


def load_report(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8-sig"))


def result_map(report: dict[str, Any]) -> dict[tuple[str, int], dict[str, Any]]:
    return {
        (str(result["case_id"]), int(result["iteration"])): result
        for result in report.get("results", [])
    }


def exact_mcnemar_p_value(baseline_only: int, candidate_only: int) -> float:
    discordant = baseline_only + candidate_only
    if discordant == 0:
        return 1.0
    tail = sum(math.comb(discordant, k) for k in range(0, min(baseline_only, candidate_only) + 1))
    return min(1.0, 2.0 * tail / (2**discordant))


def parity_checks(baseline: dict[str, Any], candidate: dict[str, Any]) -> list[tuple[str, bool, str]]:
    request_fingerprint = baseline.get("request_fingerprint")
    candidate_request_fingerprint = candidate.get("request_fingerprint")
    evaluation_fingerprint = baseline.get("evaluation_fingerprint")
    candidate_evaluation_fingerprint = candidate.get("evaluation_fingerprint")
    checks = [
        ("Case count", baseline.get("cases") == candidate.get("cases"), f"{baseline.get('cases')} vs {candidate.get('cases')}"),
        ("Repeat", baseline.get("repeat") == candidate.get("repeat"), f"{baseline.get('repeat')} vs {candidate.get('repeat')}"),
        ("Warmup", baseline.get("warmup") == candidate.get("warmup"), f"{baseline.get('warmup')} vs {candidate.get('warmup')}"),
        ("Request fingerprint", bool(request_fingerprint) and request_fingerprint == candidate_request_fingerprint, "same canonical case ID + payload"),
        ("Evaluation fingerprint", bool(evaluation_fingerprint) and evaluation_fingerprint == candidate_evaluation_fingerprint, "same final expectations"),
    ]
    for field, label in (
        ("data_snapshot", "Data snapshot"),
        ("provider_config", "Provider/model config"),
        ("cache_regime", "Cache regime"),
    ):
        left = baseline.get(field, "unspecified")
        right = candidate.get(field, "unspecified")
        checks.append((label, left == right and left != "unspecified", f"{left} vs {right}"))

    for report, label in ((baseline, "Baseline execution parity"), (candidate, "Candidate execution parity")):
        summary = report.get("summary", {})
        checks.append((
            label,
            summary.get("executed_runs") == summary.get("total") and summary.get("execution_target_met") is True,
            f"{summary.get('executed_runs')}/{summary.get('total')}",
        ))
    return checks


def render(baseline: dict[str, Any], candidate: dict[str, Any]) -> str:
    baseline_results = result_map(baseline)
    candidate_results = result_map(candidate)
    same_pairs = baseline_results.keys() == candidate_results.keys()
    checks = parity_checks(baseline, candidate)
    checks.append(("Paired case/iteration keys", same_pairs, f"{len(baseline_results)} vs {len(candidate_results)}"))
    controlled = all(passed for _, passed, _ in checks)

    shared_keys = sorted(baseline_results.keys() & candidate_results.keys())
    pass_pass = fail_pass = pass_fail = fail_fail = 0
    latency_ratios: list[float] = []
    for key in shared_keys:
        left = baseline_results[key]
        right = candidate_results[key]
        left_passed = bool(left.get("passed"))
        right_passed = bool(right.get("passed"))
        if left_passed and right_passed:
            pass_pass += 1
        elif not left_passed and right_passed:
            fail_pass += 1
        elif left_passed and not right_passed:
            pass_fail += 1
        else:
            fail_fail += 1
        right_latency = float(right.get("latency_ms") or 0.0)
        if right_latency > 0:
            latency_ratios.append(float(left.get("latency_ms") or 0.0) / right_latency)

    left_summary = baseline["summary"]
    right_summary = candidate["summary"]
    pass_delta = float(right_summary["pass_rate"]) - float(left_summary["pass_rate"])
    p_value = exact_mcnemar_p_value(pass_fail, fail_pass)
    latency_ratio = median(latency_ratios) if latency_ratios else 0.0
    protocol = "Controlled paired A/B" if controlled else "Execution-parity comparison; not yet controlled A/B"

    lines = [
        "# Paired A/B Evaluation Report",
        "",
        f"- Baseline variant: `{baseline.get('variant', 'unspecified')}`",
        f"- Candidate variant: `{candidate.get('variant', 'unspecified')}`",
        f"- Protocol verdict: **{protocol}**",
        "",
        "## Validity checks",
        "",
        "| Requirement | Result | Evidence |",
        "|---|---:|---|",
    ]
    for name, passed, detail in checks:
        lines.append(f"| {name} | {'PASS' if passed else 'FAIL'} | {detail} |")

    lines.extend([
        "",
        "## Paired result",
        "",
        "| Metric | Baseline | Candidate | Effect |",
        "|---|---:|---:|---:|",
        f"| Pass rate | {left_summary['pass_rate'] * 100:.1f}% | {right_summary['pass_rate'] * 100:.1f}% | {pass_delta * 100:+.1f} pp |",
        f"| Final executions | {left_summary['executed_runs']}/{left_summary['total']} | {right_summary['executed_runs']}/{right_summary['total']} | — |",
        f"| Confirmation rate | {left_summary['confirmation_rate'] * 100:.1f}% | {right_summary['confirmation_rate'] * 100:.1f}% | {(right_summary['confirmation_rate'] - left_summary['confirmation_rate']) * 100:+.1f} pp |",
        f"| Median latency | {left_summary['latency_median_ms']:.1f} ms | {right_summary['latency_median_ms']:.1f} ms | paired median ratio {latency_ratio:.2f}x |",
        "",
        "| Transition | Runs |",
        "|---|---:|",
        f"| PASS → PASS | {pass_pass} |",
        f"| FAIL → PASS | {fail_pass} |",
        f"| PASS → FAIL | {pass_fail} |",
        f"| FAIL → FAIL | {fail_fail} |",
        "",
        f"McNemar exact two-sided p-value on paired pass/fail outcomes: `{p_value:.4f}`.",
        "",
        "## Conclusion boundary",
        "",
    ])
    if controlled:
        lines.append(
            f"Under the declared controls, the candidate changed pass rate by {pass_delta * 100:+.1f} percentage points. "
            f"The paired result has {fail_pass} regressions fixed and {pass_fail} new regressions."
        )
    else:
        failed = ", ".join(name for name, passed, _ in checks if not passed)
        lines.append(
            "Do not make a causal A/B claim from these artifacts yet. "
            f"Unmet controls: {failed}. Report observed differences only."
        )
    lines.append("")
    return "\n".join(lines)


def main() -> int:
    args = parse_args()
    baseline = load_report(args.baseline)
    candidate = load_report(args.candidate)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(render(baseline, candidate), encoding="utf-8")
    print(f"A/B report written to {args.output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
