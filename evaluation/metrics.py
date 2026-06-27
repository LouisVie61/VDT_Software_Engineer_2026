from __future__ import annotations

from dataclasses import dataclass
from collections import Counter, defaultdict
from statistics import median
from typing import Any


@dataclass(frozen=True)
class CheckResult:
    name: str
    passed: bool
    detail: str = ""


def get_path(value: Any, path: str) -> Any:
    current = value
    for part in path.split("."):
        if isinstance(current, dict):
            current = current.get(part)
            continue
        if isinstance(current, list) and part.isdigit():
            index = int(part)
            current = current[index] if index < len(current) else None
            continue
        return None
    return current


def percentile(values: list[float], percentile_rank: float) -> float:
    if not values:
        return 0.0
    ordered = sorted(values)
    if len(ordered) == 1:
        return ordered[0]
    rank = (len(ordered) - 1) * percentile_rank
    lower = int(rank)
    upper = min(lower + 1, len(ordered) - 1)
    weight = rank - lower
    return ordered[lower] * (1 - weight) + ordered[upper] * weight


def summarize_runs(results: list[dict[str, Any]]) -> dict[str, Any]:
    total = len(results)
    passed = sum(1 for result in results if result["passed"])
    failed = total - passed
    latencies = [result["latency_ms"] for result in results if result.get("latency_ms") is not None]
    status_errors = sum(1 for result in results if result.get("status") != result.get("expected_status"))
    executed = [result for result in results if result.get("status") == 200 and result.get("needs_confirmation") is not True]
    zero_result_runs = sum(
        1
        for result in executed
        if result.get("total_count") == 0
        or (result.get("result_count", 0) == 0 and result.get("aggregation_count", 0) == 0)
    )
    confirmation_runs = sum(1 for result in results if result.get("needs_confirmation") is True)
    cache_hits = sum(1 for result in results if result.get("cache_hit") is True)
    template_counts = Counter(str(result.get("selected_template") or "none") for result in results)
    category_counts = Counter(str(result.get("category") or "uncategorized") for result in results)
    category_passed = Counter(str(result.get("category") or "uncategorized") for result in results if result["passed"])
    category_latencies: dict[str, list[float]] = defaultdict(list)
    metric_group_counts: Counter[str] = Counter()
    metric_group_pass_counts: Counter[str] = Counter()
    metric_group_latencies: dict[str, list[float]] = defaultdict(list)
    for result in results:
        if result.get("latency_ms") is not None:
            category_latencies[str(result.get("category") or "uncategorized")].append(result["latency_ms"])
        for group in result.get("metric_groups") or ["uncategorized"]:
            group = str(group)
            metric_group_counts[group] += 1
            if metric_group_passed(result, group):
                metric_group_pass_counts[group] += 1
            if result.get("latency_ms") is not None:
                metric_group_latencies[group].append(result["latency_ms"])

    return {
        "total": total,
        "passed": passed,
        "failed": failed,
        "pass_rate": passed / total if total else 0.0,
        "status_error_rate": status_errors / total if total else 0.0,
        "executed_runs": len(executed),
        "confirmation_runs": confirmation_runs,
        "confirmation_rate": confirmation_runs / total if total else 0.0,
        "cache_hits": cache_hits,
        "cache_hit_rate": cache_hits / total if total else 0.0,
        "zero_result_runs": zero_result_runs,
        "zero_result_rate": zero_result_runs / len(executed) if executed else 0.0,
        "latency_min_ms": min(latencies) if latencies else 0.0,
        "latency_median_ms": median(latencies) if latencies else 0.0,
        "latency_p95_ms": percentile(latencies, 0.95),
        "latency_max_ms": max(latencies) if latencies else 0.0,
        "template_counts": dict(sorted(template_counts.items())),
        "category_counts": dict(sorted(category_counts.items())),
        "category_pass_rates": {
            category: category_passed[category] / count
            for category, count in sorted(category_counts.items())
        },
        "category_latency_p95_ms": {
            category: percentile(values, 0.95)
            for category, values in sorted(category_latencies.items())
        },
        "metric_group_counts": dict(sorted(metric_group_counts.items())),
        "metric_group_pass_rates": {
            group: metric_group_pass_counts[group] / count
            for group, count in sorted(metric_group_counts.items())
        },
        "metric_group_latency_p95_ms": {
            group: percentile(values, 0.95)
            for group, values in sorted(metric_group_latencies.items())
        },
    }


def metric_group_passed(result: dict[str, Any], group: str) -> bool:
    checks = result.get("checks") or []
    common_names = {"http_status", "json_response"}

    def relevant(name: str) -> bool:
        if name in common_names:
            return True
        if group == "Performance":
            return False
        if group == "DSL Correctness":
            return (
                "generatedDsl" in name
                or name.startswith("dsl_")
                or name == "valid_generated_dsl"
                or name in {"non_empty:generatedDsl", "equals:nlQuery"}
            )
        if group == "Aggregation Correctness":
            return (
                "generatedDsl" in name
                or name.startswith("dsl_")
                or name in {"valid_generated_dsl", "aggregation_dsl", "aggregation_evidence"}
                or name == "non_empty:aggregations"
            )
        if group == "Result Quality":
            return name in {
                "min_total_count", "max_total_count", "execution_evidence",
                "non_zero_total_count", "non_empty_execution_payload",
                "non_empty:results", "non_empty:aggregations", "aggregation_evidence",
            }
        if group == "Safety / Guardrail":
            return (
                name.startswith("dsl_not_contains:")
                or name.startswith("field_not_exists:")
                or name.startswith("generatedDsl_path_not_exists:")
            )
        return True

    selected = [check for check in checks if relevant(check.name)]
    return bool(selected) and all(check.passed for check in selected)


def format_ms(value: float) -> str:
    return f"{value:.1f} ms"


def format_rate(value: float) -> str:
    return f"{value * 100:.1f}%"
