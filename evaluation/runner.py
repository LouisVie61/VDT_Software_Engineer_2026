from __future__ import annotations

import argparse
import hashlib
import json
import platform
import sys
from datetime import datetime, timezone
from pathlib import Path

from evaluator import load_cases, run_case
from metrics import format_ms, format_rate, summarize_runs


DEFAULT_CASES = [
    Path("evaluation/cases/workflow/soc_nl2plan_v1.jsonl"),
    Path("evaluation/cases/workflow/ambiguity_cases.jsonl"),
    Path("evaluation/cases/workflow/llm_language_cases.jsonl"),
    Path("evaluation/cases/workflow/temporal_cases.jsonl"),
    Path("evaluation/cases/regression/residual_cases.jsonl"),
]


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Run backend evaluation benchmark and write a markdown report.")
    parser.add_argument("--base-url", default="http://localhost:8080", help="Backend base URL.")
    parser.add_argument("--cases", nargs="*", type=Path, default=DEFAULT_CASES, help="JSONL case files.")
    parser.add_argument("--repeat", type=int, default=1, help="Number of iterations per case.")
    parser.add_argument("--timeout", type=float, default=10.0, help="HTTP timeout in seconds.")
    parser.add_argument("--warmup", type=int, default=0, help="Warmup iterations per case. Warmup runs are not included in the report.")
    parser.add_argument("--output", type=Path, default=Path("evaluation/reports/backend_evaluation_report.md"), help="Markdown report path.")
    parser.add_argument("--json-output", type=Path, default=None, help="Optional machine-readable JSON report path.")
    parser.add_argument("--min-pass-rate", type=float, default=0.90, help="Minimum acceptable pass rate, from 0 to 1.")
    parser.add_argument("--variant", default="unspecified", help="Stable label for the backend variant under test.")
    parser.add_argument("--data-snapshot", default="unspecified", help="Shared Elasticsearch snapshot/version label for A/B validation.")
    parser.add_argument("--provider-config", default="unspecified", help="Shared provider/model/config label for A/B validation.")
    parser.add_argument("--cache-regime", choices=("unspecified", "cold", "warm", "mixed"), default="unspecified", help="Cache state used by this run.")
    parser.add_argument("--auto-confirm", action="store_true", help="Follow needsConfirmation responses through /api/search/confirm.")
    parser.add_argument("--require-executions", type=int, default=None, help="Require exactly this many finalized executions.")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    cases = load_cases(args.cases)
    results = []

    for iteration in range(1, args.warmup + 1):
        for case in cases:
            result = run_case(case, args.base_url, args.timeout, iteration, auto_confirm=args.auto_confirm)
            status = "PASS" if result["passed"] else "FAIL"
            print(f"[WARMUP {status}] iter={iteration} case={case.id} latency={result['latency_ms']:.1f}ms")

    for iteration in range(1, args.repeat + 1):
        for case in cases:
            result = run_case(case, args.base_url, args.timeout, iteration, auto_confirm=args.auto_confirm)
            results.append(result)
            status = "PASS" if result["passed"] else "FAIL"
            print(f"[{status}] iter={iteration} case={case.id} latency={result['latency_ms']:.1f}ms")

    summary = summarize_runs(results)
    summary["variant"] = args.variant
    summary["request_fingerprint"] = cases_fingerprint(cases, include_expectations=False)
    summary["evaluation_fingerprint"] = cases_fingerprint(cases, include_expectations=True)
    summary["required_executions"] = args.require_executions
    summary["execution_target_met"] = (
        args.require_executions is None or summary["executed_runs"] == args.require_executions
    )
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(render_report(args, cases, results, summary), encoding="utf-8")
    if args.json_output is not None:
        args.json_output.parent.mkdir(parents=True, exist_ok=True)
        args.json_output.write_text(render_json_report(args, cases, results, summary), encoding="utf-8")

    print(f"\nReport written to {args.output}")
    if args.json_output is not None:
        print(f"JSON report written to {args.json_output}")
    return 0 if benchmark_passed(summary, args.min_pass_rate) else 1


def benchmark_passed(summary: dict, min_pass_rate: float) -> bool:
    return summary["pass_rate"] >= min_pass_rate and summary.get("execution_target_met", True)


def cases_fingerprint(cases: list, include_expectations: bool) -> str:
    payload = []
    for case in cases:
        item = {"id": case.id, "request": case.request}
        if include_expectations:
            item["expected"] = case.expected
        payload.append(item)
    encoded = json.dumps(payload, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode("utf-8")
    return hashlib.sha256(encoded).hexdigest()


def render_json_report(args: argparse.Namespace, cases: list, results: list[dict], summary: dict) -> str:
    payload = {
        "base_url": args.base_url,
        "cases": len(cases),
        "repeat": args.repeat,
        "warmup": args.warmup,
        "variant": args.variant,
        "data_snapshot": args.data_snapshot,
        "provider_config": args.provider_config,
        "cache_regime": args.cache_regime,
        "request_fingerprint": summary["request_fingerprint"],
        "evaluation_fingerprint": summary["evaluation_fingerprint"],
        "min_pass_rate": args.min_pass_rate,
        "summary": summary,
        "results": [
            {
                **result,
                "checks": [check.__dict__ for check in result["checks"]],
            }
            for result in results
        ],
    }
    return json.dumps(payload, indent=2, sort_keys=True)


def render_report(args: argparse.Namespace, cases: list, results: list[dict], summary: dict) -> str:
    generated_at = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%S UTC")
    verdict = "PASS" if benchmark_passed(summary, args.min_pass_rate) else "FAIL"
    lines = [
        "# Backend Evaluation Report",
        "",
        f"- Generated at: `{generated_at}`",
        f"- Backend: `{args.base_url}`",
        f"- Python: `{platform.python_version()}`",
        f"- Cases: `{len(cases)}`",
        f"- Repeat: `{args.repeat}`",
        f"- Warmup: `{args.warmup}`",
        f"- Variant: `{args.variant}`",
        f"- Data snapshot: `{args.data_snapshot}`",
        f"- Provider config: `{args.provider_config}`",
        f"- Cache regime: `{args.cache_regime}`",
        f"- Request fingerprint: `{summary['request_fingerprint']}`",
        f"- Evaluation fingerprint: `{summary['evaluation_fingerprint']}`",
        f"- Verdict: **{verdict}**",
        "",
        "## Summary",
        "",
        "| Metric | Value |",
        "|---|---:|",
        f"| Total runs | {summary['total']} |",
        f"| Passed | {summary['passed']} |",
        f"| Failed | {summary['failed']} |",
        f"| Pass rate | {format_rate(summary['pass_rate'])} |",
        f"| Status error rate | {format_rate(summary['status_error_rate'])} |",
        f"| Executed runs | {summary['executed_runs']} |",
        f"| Required executions | {summary['required_executions'] if summary['required_executions'] is not None else '-'} |",
        f"| Execution target met | {summary['execution_target_met']} |",
        f"| Confirmation runs | {summary['confirmation_runs']} |",
        f"| Confirmations followed | {summary['confirmation_followed_runs']} |",
        f"| Confirmation rate | {format_rate(summary['confirmation_rate'])} |",
        f"| Cache hits | {summary['cache_hits']} |",
        f"| Cache hit rate | {format_rate(summary['cache_hit_rate'])} |",
        f"| Zero-result runs | {summary['zero_result_runs']} |",
        f"| Zero-result rate | {format_rate(summary['zero_result_rate'])} |",
        f"| Latency min | {format_ms(summary['latency_min_ms'])} |",
        f"| Latency median | {format_ms(summary['latency_median_ms'])} |",
        f"| Latency p95 | {format_ms(summary['latency_p95_ms'])} |",
        f"| Latency max | {format_ms(summary['latency_max_ms'])} |",
        "",
        "## Category Breakdown",
        "",
        "| Category | Runs | Pass Rate | Latency p95 |",
        "|---|---:|---:|---:|",
    ]

    for category, count in summary["category_counts"].items():
        lines.append(
            f"| {category} | {count} | {format_rate(summary['category_pass_rates'].get(category, 0.0))} | "
            f"{format_ms(summary['category_latency_p95_ms'].get(category, 0.0))} |"
        )

    lines.extend([
        "",
        "## Metric Group Scorecard",
        "",
        "| Metric group | Runs | Run pass rate | Checks | Check pass rate | Latency p95 |",
        "|---|---:|---:|---:|---:|---:|",
    ])
    metric_group_order = [
        "DSL Correctness",
        "Aggregation Correctness",
        "Result Quality",
        "Safety / Guardrail",
        "Performance",
    ]
    for group in metric_group_order:
        count = summary["metric_group_run_counts"].get(group, 0)
        if count == 0:
            continue
        lines.append(
            f"| {group} | {count} | {format_rate(summary['metric_group_run_pass_rates'].get(group, 0.0))} | "
            f"{summary['metric_group_check_counts'].get(group, 0)} | "
            f"{format_rate(summary['metric_group_check_pass_rates'].get(group, 0.0))} | "
            f"{format_ms(summary['metric_group_latency_p95_ms'].get(group, 0.0))} |"
        )

    lines.extend([
        "",
        "## Template Distribution",
        "",
        "| Template | Runs |",
        "|---|---:|",
    ])
    for template, count in summary["template_counts"].items():
        lines.append(f"| {template} | {count} |")

    lines.extend([
        "",
        "## Case Results",
        "",
        "| Case | Category | Tags | Iteration | Status | Template | Confirm | Cache | Total | Rows | Aggs | Latency | Result | Failed Checks |",
        "|---|---|---|---:|---:|---|---:|---:|---:|---:|---:|---:|---|---|",
    ])

    for result in results:
        failed_checks = [check for check in result["checks"] if not check.passed]
        failed_text = "<br>".join(f"`{check.name}` {check.detail}" for check in failed_checks)
        if result["error"]:
            failed_text = (failed_text + "<br>" if failed_text else "") + result["error"]
        lines.append(
            "| {case_id} | {category} | {tags} | {iteration} | {status} | {template} | {confirm} | {cache} | {total} | {rows} | {aggs} | {latency} | {result} | {failed} |".format(
                case_id=result["case_id"],
                category=result["category"],
                tags=", ".join(result.get("tags") or []) or "-",
                iteration=result["iteration"],
                status=result["status"],
                template=result.get("selected_template") or "-",
                confirm=result.get("initial_needs_confirmation", result.get("needs_confirmation")),
                cache=result.get("cache_hit"),
                total=result.get("total_count"),
                rows=result.get("result_count", 0),
                aggs=result.get("aggregation_count", 0),
                latency=format_ms(result["latency_ms"]),
                result="PASS" if result["passed"] else "FAIL",
                failed=failed_text or "-",
            )
        )

    for path in args.cases:
        lines.append(f"- `{path}`")

    lines.append("")
    return "\n".join(lines)


if __name__ == "__main__":
    sys.exit(main())
