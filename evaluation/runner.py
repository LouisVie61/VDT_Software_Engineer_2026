from __future__ import annotations

import argparse
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
    parser.add_argument("--timeout", type=float, default=300.0, help="HTTP transport guard in seconds; it is not a pass/fail latency threshold.")
    parser.add_argument("--warmup", type=int, default=0, help="Warmup iterations per case. Warmup runs are not included in the report.")
    parser.add_argument("--output", type=Path, default=Path("evaluation/reports/backend_evaluation_report.md"), help="Markdown report path.")
    parser.add_argument("--json-output", type=Path, default=None, help="Optional machine-readable JSON report path.")
    parser.add_argument("--min-pass-rate", type=float, default=0.90, help="Minimum acceptable pass rate, from 0 to 1.")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    cases = load_cases(args.cases)
    results = []

    for iteration in range(1, args.warmup + 1):
        for case in cases:
            result = run_case(case, args.base_url, args.timeout, iteration)
            status = "PASS" if result["passed"] else "FAIL"
            print(f"[WARMUP {status}] iter={iteration} case={case.id} latency={result['latency_ms']:.1f}ms")

    for iteration in range(1, args.repeat + 1):
        for case in cases:
            result = run_case(case, args.base_url, args.timeout, iteration)
            results.append(result)
            status = "PASS" if result["passed"] else "FAIL"
            print(f"[{status}] iter={iteration} case={case.id} latency={result['latency_ms']:.1f}ms")

    summary = summarize_runs(results)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(render_report(args, cases, results, summary), encoding="utf-8")
    if args.json_output is not None:
        args.json_output.parent.mkdir(parents=True, exist_ok=True)
        args.json_output.write_text(render_json_report(args, cases, results, summary), encoding="utf-8")

    print(f"\nReport written to {args.output}")
    if args.json_output is not None:
        print(f"JSON report written to {args.json_output}")
    return 0 if summary["pass_rate"] >= args.min_pass_rate else 1


def render_json_report(args: argparse.Namespace, cases: list, results: list[dict], summary: dict) -> str:
    payload = {
        "base_url": args.base_url,
        "cases": len(cases),
        "repeat": args.repeat,
        "warmup": args.warmup,
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
    verdict = "PASS" if summary["pass_rate"] >= args.min_pass_rate else "FAIL"
    lines = [
        "# Backend Evaluation Report",
        "",
        f"- Generated at: `{generated_at}`",
        f"- Backend: `{args.base_url}`",
        f"- Python: `{platform.python_version()}`",
        f"- Cases: `{len(cases)}`",
        f"- Repeat: `{args.repeat}`",
        f"- Warmup: `{args.warmup}`",
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
        f"| Confirmation runs | {summary['confirmation_runs']} |",
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
        "| Metric group | Runs | Pass rate | Latency p95 |",
        "|---|---:|---:|---:|",
    ])
    for group, count in summary["metric_group_counts"].items():
        lines.append(
            f"| {group} | {count} | {format_rate(summary['metric_group_pass_rates'].get(group, 0.0))} | "
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
                confirm=result.get("needs_confirmation"),
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
