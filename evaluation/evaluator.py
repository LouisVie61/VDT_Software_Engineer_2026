from __future__ import annotations

import json
import time
import urllib.error
import urllib.request
from dataclasses import dataclass
from pathlib import Path
from typing import Any

from metrics import CheckResult, get_path


@dataclass(frozen=True)
class EvaluationCase:
    source: str
    id: str
    category: str
    description: str
    tags: list[str]
    request: dict[str, Any]
    expected: dict[str, Any]
    thresholds: dict[str, Any]


def load_cases(paths: list[Path]) -> list[EvaluationCase]:
    cases: list[EvaluationCase] = []
    for path in paths:
        with path.open("r", encoding="utf-8-sig") as handle:
            for line_number, line in enumerate(handle, start=1):
                stripped = line.strip()
                if not stripped:
                    continue
                raw = json.loads(stripped)
                cases.append(
                    EvaluationCase(
                        source=f"{path.name}:{line_number}",
                        id=raw["id"],
                        category=raw.get("category", "uncategorized"),
                        description=raw.get("description", ""),
                        tags=raw.get("tags", []),
                        request=raw["request"],
                        expected=raw.get("expected", {}),
                        thresholds=raw.get("thresholds", {}),
                    )
                )
    return cases


def run_case(case: EvaluationCase, base_url: str, timeout: float, iteration: int) -> dict[str, Any]:
    url = base_url.rstrip("/") + "/api/search"
    payload = json.dumps(case.request).encode("utf-8")
    request = urllib.request.Request(
        url,
        data=payload,
        headers={"Content-Type": "application/json", "Accept": "application/json"},
        method="POST",
    )

    started = time.perf_counter()
    status = None
    response_json: dict[str, Any] | None = None
    response_text = ""
    error = ""

    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            status = response.status
            response_text = response.read().decode("utf-8")
            response_json = json.loads(response_text) if response_text else {}
    except urllib.error.HTTPError as exc:
        status = exc.code
        response_text = exc.read().decode("utf-8", errors="replace")
        try:
            response_json = json.loads(response_text) if response_text else {}
        except json.JSONDecodeError:
            response_json = None
        error = str(exc)
    except Exception as exc:  # noqa: BLE001 - benchmark must report connection/runtime failures.
        error = str(exc)

    latency_ms = (time.perf_counter() - started) * 1000
    checks = evaluate_expectations(case, status, response_json, response_text, latency_ms)
    passed = all(check.passed for check in checks)
    response_stats = execution_stats(response_json)

    return {
        "case_id": case.id,
        "source": case.source,
        "category": case.category,
        "tags": case.tags,
        "description": case.description,
        "iteration": iteration,
        "status": status,
        "expected_status": case.expected.get("status", 200),
        "latency_ms": latency_ms,
        "total_count": response_stats["total_count"],
        "result_count": response_stats["result_count"],
        "aggregation_count": response_stats["aggregation_count"],
        "selected_template": response_stats["selected_template"],
        "needs_confirmation": response_stats["needs_confirmation"],
        "cache_hit": response_stats["cache_hit"],
        "summary_status": response_stats["summary_status"],
        "passed": passed,
        "checks": checks,
        "error": error,
    }


def evaluate_expectations(
    case: EvaluationCase,
    status: int | None,
    response_json: dict[str, Any] | None,
    response_text: str,
    latency_ms: float,
) -> list[CheckResult]:
    expected = case.expected
    thresholds = case.thresholds
    checks: list[CheckResult] = []

    expected_status = expected.get("status", 200)
    checks.append(CheckResult("http_status", status == expected_status, f"expected={expected_status}, actual={status}"))

    if response_json is None:
        checks.append(CheckResult("json_response", False, "response is not valid JSON"))
        return checks

    for field in expected.get("responseFields", []):
        checks.append(CheckResult(f"field:{field}", get_path(response_json, field) is not None, "missing field"))

    for field, expected_value in expected.get("equals", {}).items():
        actual = get_path(response_json, field)
        checks.append(CheckResult(f"equals:{field}", actual == expected_value, f"expected={expected_value!r}, actual={actual!r}"))

    for field, expected_value in expected.get("notEquals", {}).items():
        actual = get_path(response_json, field)
        checks.append(CheckResult(f"not_equals:{field}", actual != expected_value, f"not_expected={expected_value!r}, actual={actual!r}"))

    for field, values in expected.get("fieldIn", {}).items():
        actual = get_path(response_json, field)
        checks.append(CheckResult(f"field_in:{field}", actual in values, f"expected one of={values!r}, actual={actual!r}"))

    for field in expected.get("fieldExists", []):
        checks.append(CheckResult(f"field_exists:{field}", get_path(response_json, field) is not None, "missing field"))

    for field in expected.get("fieldNotExists", []):
        checks.append(CheckResult(f"field_not_exists:{field}", get_path(response_json, field) is None, "field exists"))

    for field in expected.get("nonEmptyFields", []):
        actual = get_path(response_json, field)
        passed = actual is not None and actual != "" and actual != [] and actual != {}
        checks.append(CheckResult(f"non_empty:{field}", passed, f"actual={actual!r}"))

    for field in ("needsConfirmation", "selectedTemplate", "chartType", "summaryStatus"):
        if field in expected:
            actual = get_path(response_json, field)
            checks.append(CheckResult(f"equals:{field}", actual == expected[field], f"expected={expected[field]!r}, actual={actual!r}"))

    total_count = get_path(response_json, "totalCount")
    confirmation_response = get_path(response_json, "needsConfirmation") is True
    if "minTotalCount" in expected and not confirmation_response:
        checks.append(CheckResult("min_total_count", isinstance(total_count, int) and total_count >= expected["minTotalCount"], f"actual={total_count!r}"))
    if "maxTotalCount" in expected and not confirmation_response:
        checks.append(CheckResult("max_total_count", isinstance(total_count, int) and total_count <= expected["maxTotalCount"], f"actual={total_count!r}"))
    if expected_status == 200 and not expected.get("allowZeroResults", False):
        checks.extend(execution_evidence_checks(response_json))

    dsl_text = json.dumps(response_json.get("generatedDsl"), sort_keys=True, separators=(",", ":"))
    generated_dsl = response_json.get("generatedDsl") if isinstance(response_json.get("generatedDsl"), dict) else {}
    checks.extend(path_expectations("generatedDsl", generated_dsl, expected.get("generatedDslPath", {})))
    for fragment in expected.get("generatedDslContains", []):
        checks.append(CheckResult(f"dsl_contains:{fragment}", fragment in dsl_text, "fragment not found"))
    for fragment in expected.get("generatedDslNotContains", []):
        checks.append(CheckResult(f"dsl_not_contains:{fragment}", fragment not in dsl_text, "fragment was found"))
    for fragments in expected.get("generatedDslAnyContains", []):
        checks.append(CheckResult(f"dsl_any_contains:{fragments}", any(fragment in dsl_text for fragment in fragments), "no alternative fragment found"))

    if "responseContains" in expected:
        compact_response = json.dumps(response_json, sort_keys=True, separators=(",", ":"))
        for fragment in expected["responseContains"]:
            checks.append(CheckResult(f"response_contains:{fragment}", fragment in compact_response, "fragment not found"))

    if "anyOf" in expected:
        checks.append(any_of_check(expected["anyOf"], response_json))

    if "max_latency_ms" in thresholds:
        max_latency_ms = float(thresholds["max_latency_ms"])
        checks.append(CheckResult("latency_budget", latency_ms <= max_latency_ms, f"budget={max_latency_ms:.1f} ms, actual={latency_ms:.1f} ms"))

    return checks


def any_of_check(branches: list[dict[str, Any]], response_json: dict[str, Any]) -> CheckResult:
    failures: list[str] = []
    for index, branch in enumerate(branches, start=1):
        branch_checks = evaluate_branch(branch, response_json)
        if all(check.passed for check in branch_checks):
            return CheckResult("any_of", True, f"matched branch {index}")
        failed = ", ".join(f"{check.name}: {check.detail}" for check in branch_checks if not check.passed)
        failures.append(f"branch {index} failed [{failed}]")
    return CheckResult("any_of", False, "; ".join(failures))


def evaluate_branch(expected: dict[str, Any], response_json: dict[str, Any]) -> list[CheckResult]:
    checks: list[CheckResult] = []

    for field in expected.get("responseFields", []):
        checks.append(CheckResult(f"field:{field}", get_path(response_json, field) is not None, "missing field"))

    for field, expected_value in expected.get("equals", {}).items():
        actual = get_path(response_json, field)
        checks.append(CheckResult(f"equals:{field}", actual == expected_value, f"expected={expected_value!r}, actual={actual!r}"))

    for field, expected_value in expected.get("notEquals", {}).items():
        actual = get_path(response_json, field)
        checks.append(CheckResult(f"not_equals:{field}", actual != expected_value, f"not_expected={expected_value!r}, actual={actual!r}"))

    for field, values in expected.get("fieldIn", {}).items():
        actual = get_path(response_json, field)
        checks.append(CheckResult(f"field_in:{field}", actual in values, f"expected one of={values!r}, actual={actual!r}"))

    for field in expected.get("fieldExists", []):
        checks.append(CheckResult(f"field_exists:{field}", get_path(response_json, field) is not None, "missing field"))

    for field in expected.get("fieldNotExists", []):
        checks.append(CheckResult(f"field_not_exists:{field}", get_path(response_json, field) is None, "field exists"))

    for field in expected.get("nonEmptyFields", []):
        actual = get_path(response_json, field)
        passed = actual is not None and actual != "" and actual != [] and actual != {}
        checks.append(CheckResult(f"non_empty:{field}", passed, f"actual={actual!r}"))

    for field in ("needsConfirmation", "selectedTemplate", "chartType", "summaryStatus"):
        if field in expected:
            actual = get_path(response_json, field)
            checks.append(CheckResult(f"equals:{field}", actual == expected[field], f"expected={expected[field]!r}, actual={actual!r}"))

    total_count = get_path(response_json, "totalCount")
    if "minTotalCount" in expected:
        checks.append(CheckResult("min_total_count", isinstance(total_count, int) and total_count >= expected["minTotalCount"], f"actual={total_count!r}"))
    if "maxTotalCount" in expected:
        checks.append(CheckResult("max_total_count", isinstance(total_count, int) and total_count <= expected["maxTotalCount"], f"actual={total_count!r}"))

    dsl_text = json.dumps(response_json.get("generatedDsl"), sort_keys=True, separators=(",", ":"))
    generated_dsl = response_json.get("generatedDsl") if isinstance(response_json.get("generatedDsl"), dict) else {}
    checks.extend(path_expectations("generatedDsl", generated_dsl, expected.get("generatedDslPath", {})))
    for fragment in expected.get("generatedDslContains", []):
        checks.append(CheckResult(f"dsl_contains:{fragment}", fragment in dsl_text, "fragment not found"))
    for fragment in expected.get("generatedDslNotContains", []):
        checks.append(CheckResult(f"dsl_not_contains:{fragment}", fragment not in dsl_text, "fragment was found"))
    for fragments in expected.get("generatedDslAnyContains", []):
        checks.append(CheckResult(f"dsl_any_contains:{fragments}", any(fragment in dsl_text for fragment in fragments), "no alternative fragment found"))

    if "responseContains" in expected:
        compact_response = json.dumps(response_json, sort_keys=True, separators=(",", ":"))
        for fragment in expected["responseContains"]:
            checks.append(CheckResult(f"response_contains:{fragment}", fragment in compact_response, "fragment not found"))

    if expected.get("requiresExecutionEvidence", False):
        checks.extend(execution_evidence_checks(response_json))

    return checks


def path_expectations(prefix: str, payload: dict[str, Any], expected: dict[str, Any]) -> list[CheckResult]:
    checks: list[CheckResult] = []
    for field, expected_value in expected.get("equals", {}).items():
        actual = get_path(payload, field)
        checks.append(CheckResult(f"{prefix}_path_equals:{field}", actual == expected_value, f"expected={expected_value!r}, actual={actual!r}"))
    for field, values in expected.get("fieldIn", {}).items():
        actual = get_path(payload, field)
        checks.append(CheckResult(f"{prefix}_path_in:{field}", actual in values, f"expected one of={values!r}, actual={actual!r}"))
    for field in expected.get("exists", []):
        checks.append(CheckResult(f"{prefix}_path_exists:{field}", get_path(payload, field) is not None, "missing path"))
    for field in expected.get("notExists", []):
        checks.append(CheckResult(f"{prefix}_path_not_exists:{field}", get_path(payload, field) is None, "path exists"))
    return checks


def execution_evidence_checks(response_json: dict[str, Any]) -> list[CheckResult]:
    stats = execution_stats(response_json)
    if stats["needs_confirmation"] is True:
        return [
            CheckResult(
                "execution_evidence",
                True,
                "confirmation response does not execute search",
            )
        ]

    total_count = stats["total_count"]
    result_count = stats["result_count"]
    aggregation_count = stats["aggregation_count"]
    has_positive_total = isinstance(total_count, int) and total_count > 0
    has_payload_evidence = result_count > 0 or aggregation_count > 0
    return [
        CheckResult(
            "non_zero_total_count",
            has_positive_total,
            f"totalCount={total_count!r}; set expected.allowZeroResults=true only for intentional empty-result cases",
        ),
        CheckResult(
            "non_empty_execution_payload",
            has_payload_evidence,
            f"results={result_count}, aggregations={aggregation_count}",
        ),
    ]


def execution_stats(response_json: dict[str, Any] | None) -> dict[str, Any]:
    if response_json is None:
        return {
            "total_count": None,
            "result_count": 0,
            "aggregation_count": 0,
            "selected_template": None,
            "needs_confirmation": None,
            "cache_hit": None,
            "summary_status": None,
        }
    return {
        "total_count": get_path(response_json, "totalCount"),
        "result_count": collection_size(get_path(response_json, "results")),
        "aggregation_count": collection_size(get_path(response_json, "aggregations")),
        "selected_template": get_path(response_json, "selectedTemplate"),
        "needs_confirmation": get_path(response_json, "needsConfirmation"),
        "cache_hit": get_path(response_json, "cacheHit"),
        "summary_status": get_path(response_json, "summaryStatus"),
    }


def collection_size(value: Any) -> int:
    if isinstance(value, list):
        return len(value)
    if isinstance(value, dict):
        return len(value)
    return 0
