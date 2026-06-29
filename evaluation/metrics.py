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
    metric_group: str = "Result Quality"


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
    executed = [result for result in results if result.get("final_execution") is True]
    zero_result_runs = sum(
        1
        for result in executed
        if result.get("total_count") == 0
        or (result.get("result_count", 0) == 0 and result.get("aggregation_count", 0) == 0)
    )
    confirmation_runs = sum(
        1 for result in results
        if result.get("initial_needs_confirmation", result.get("needs_confirmation")) is True
    )
    confirmation_followed_runs = sum(1 for result in results if result.get("confirmation_followed") is True)
    cache_hits = sum(1 for result in results if result.get("cache_hit") is True)
    template_counts = Counter(str(result.get("selected_template") or "none") for result in results)
    category_counts = Counter(str(result.get("category") or "uncategorized") for result in results)
    category_passed = Counter(str(result.get("category") or "uncategorized") for result in results if result["passed"])
    category_latencies: dict[str, list[float]] = defaultdict(list)
    metric_check_counts: Counter[str] = Counter()
    metric_check_passed: Counter[str] = Counter()
    metric_run_counts: Counter[str] = Counter()
    metric_run_passed: Counter[str] = Counter()
    metric_latencies: dict[str, list[float]] = defaultdict(list)
    for result in results:
        if result.get("latency_ms") is not None:
            category_latencies[str(result.get("category") or "uncategorized")].append(result["latency_ms"])
        checks_by_group: dict[str, list[CheckResult]] = defaultdict(list)
        for check in result.get("checks") or []:
            checks_by_group[check.metric_group].append(check)
            metric_check_counts[check.metric_group] += 1
            if check.passed:
                metric_check_passed[check.metric_group] += 1
        for group, checks in checks_by_group.items():
            metric_run_counts[group] += 1
            if all(check.passed for check in checks):
                metric_run_passed[group] += 1
            if result.get("latency_ms") is not None:
                metric_latencies[group].append(result["latency_ms"])

    return {
        "total": total,
        "passed": passed,
        "failed": failed,
        "pass_rate": passed / total if total else 0.0,
        "status_error_rate": status_errors / total if total else 0.0,
        "executed_runs": len(executed),
        "confirmation_runs": confirmation_runs,
        "confirmation_rate": confirmation_runs / total if total else 0.0,
        "confirmation_followed_runs": confirmation_followed_runs,
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
        "metric_group_check_counts": dict(sorted(metric_check_counts.items())),
        "metric_group_check_pass_rates": {
            group: metric_check_passed[group] / count
            for group, count in sorted(metric_check_counts.items())
        },
        "metric_group_run_counts": dict(sorted(metric_run_counts.items())),
        "metric_group_run_pass_rates": {
            group: metric_run_passed[group] / count
            for group, count in sorted(metric_run_counts.items())
        },
        "metric_group_latency_p95_ms": {
            group: percentile(values, 0.95)
            for group, values in sorted(metric_latencies.items())
        },
    }


def format_ms(value: float) -> str:
    return f"{value:.1f} ms"


def format_rate(value: float) -> str:
    return f"{value * 100:.1f}%"