from __future__ import annotations

import json
import time
import urllib.error
import urllib.request
from dataclasses import dataclass, replace
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
    assisted: dict[str, Any]


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
                        assisted=raw.get("assisted", {}),
                    )
                )
    return cases


def run_case(
    case: EvaluationCase,
    base_url: str,
    timeout: float,
    iteration: int,
    auto_confirm: bool = False,
) -> dict[str, Any]:
    initial = _post_json(base_url.rstrip("/") + "/api/search", case.request, timeout)
    initial_json = initial["response_json"]
    initial_stats = execution_stats(initial_json)
    final = initial
    confirmation_followed = False
    configuration_error = ""

    if auto_confirm and initial_stats["needs_confirmation"] is True:
        confirmation_followed = True
        confirm_payload, configuration_error = build_confirmation_payload(case, initial_json)
        if not configuration_error:
            final = _post_json(base_url.rstrip("/") + "/api/search/confirm", confirm_payload, timeout)

    latency_ms = initial["latency_ms"]
    if final is not initial:
        latency_ms += final["latency_ms"]

    score_final_response = auto_confirm and case.assisted.get("scoreFinalResponse") is True
    if score_final_response:
        checks: list[CheckResult] = []
        if confirmation_followed:
            initial_expected = case.assisted.get("initialExpected") or {
                "status": case.expected.get("status", 200),
                "needsConfirmation": True,
                "nonEmptyFields": ["confirmation.confirmationId", "confirmation.intent"],
                "allowZeroResults": True,
            }
            initial_case = replace(case, expected=initial_expected, thresholds={})
            checks.extend(prefix_checks(
                evaluate_expectations(
                    initial_case,
                    initial["status"],
                    initial_json,
                    initial["response_text"],
                    initial["latency_ms"],
                ),
                "initial",
            ))

        final_expected = case.assisted.get("finalExpected", case.expected)
        final_thresholds = case.assisted.get("thresholds", case.thresholds)
        evaluation_case = replace(case, expected=final_expected, thresholds=final_thresholds)
        checks.extend(prefix_checks(
            evaluate_expectations(
                evaluation_case,
                final["status"],
                final["response_json"],
                final["response_text"],
                latency_ms,
            ),
            "final",
        ))
        checks.extend(prefix_checks(
            assisted_execution_checks(final["status"], final["response_json"], configuration_error),
            "final",
        ))
    else:
        evaluation_case = assisted_evaluation_case(case, confirmation_followed) if auto_confirm else case
        checks = evaluate_expectations(
            evaluation_case,
            initial["status"],
            initial_json,
            initial["response_text"],
            latency_ms,
        )
        if auto_confirm:
            checks.extend(assisted_execution_checks(final["status"], final["response_json"], configuration_error))
    checks = assign_metric_groups(evaluation_case, checks)
    passed = all(check.passed for check in checks)
    response_json = final["response_json"]
    response_stats = execution_stats(response_json)
    final_execution = is_final_execution(final["status"], response_json)
    errors = [value for value in (initial["error"], final["error"] if final is not initial else "", configuration_error) if value]

    return {
        "case_id": case.id,
        "source": case.source,
        "category": case.category,
        "tags": case.tags,
        "description": case.description,
        "iteration": iteration,
        "status": final["status"],
        "initial_status": initial["status"],
        "confirmation_status": final["status"] if confirmation_followed and final is not initial else None,
        "expected_status": case.expected.get("status", 200),
        "latency_ms": latency_ms,
        "initial_latency_ms": initial["latency_ms"],
        "confirmation_latency_ms": final["latency_ms"] if final is not initial else 0.0,
        "total_count": response_stats["total_count"],
        "result_count": response_stats["result_count"],
        "aggregation_count": response_stats["aggregation_count"],
        "selected_template": response_stats["selected_template"],
        "needs_confirmation": response_stats["needs_confirmation"],
        "initial_needs_confirmation": initial_stats["needs_confirmation"],
        "confirmation_followed": confirmation_followed,
        "final_execution": final_execution,
        "cache_hit": response_stats["cache_hit"],
        "summary_status": response_stats["summary_status"],
        "generated_dsl": response_json.get("generatedDsl") if response_json else None,
        "passed": passed,
        "checks": checks,
        "error": "; ".join(errors),
    }


def _post_json(url: str, payload_value: dict[str, Any], timeout: float) -> dict[str, Any]:
    payload = json.dumps(payload_value).encode("utf-8")
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

    return {
        "status": status,
        "latency_ms": (time.perf_counter() - started) * 1000,
        "response_json": response_json,
        "response_text": response_text,
        "error": error,
    }


def build_confirmation_payload(
    case: EvaluationCase,
    initial_response: dict[str, Any] | None,
) -> tuple[dict[str, Any], str]:
    if initial_response is None:
        return {}, "auto-confirm requires a JSON search response"
    confirmation_id = get_path(initial_response, "confirmation.confirmationId")
    if not isinstance(confirmation_id, str) or not confirmation_id:
        return {}, "auto-confirm response is missing confirmation.confirmationId"
    pending_intent = get_path(initial_response, "confirmation.intent")
    if not isinstance(pending_intent, dict):
        return {}, "auto-confirm response is missing confirmation.intent"

    edited_intent = deep_merge(pending_intent, case.assisted.get("editedIntent", {}))
    page = case.assisted.get("page", case.request.get("page"))
    page_size = case.assisted.get("pageSize", case.request.get("pageSize"))
    payload: dict[str, Any] = {
        "confirmationId": confirmation_id,
        "editedIntent": edited_intent,
        "page": 0 if page is None else page,
        "pageSize": 50 if page_size is None else page_size,
    }
    session_id = case.request.get("sessionId")
    if session_id:
        payload["sessionId"] = session_id
    return payload, ""


def assisted_evaluation_case(case: EvaluationCase, confirmation_followed: bool) -> EvaluationCase:
    expected = case.assisted.get("initialExpected", case.expected)
    thresholds = case.assisted.get("thresholds")
    if thresholds is None:
        thresholds = dict(case.thresholds)
        if confirmation_followed and "max_latency_ms" in thresholds:
            thresholds["max_latency_ms"] = float(thresholds["max_latency_ms"]) * 2
    return replace(case, expected=expected, thresholds=thresholds)


def prefix_checks(checks: list[CheckResult], phase: str) -> list[CheckResult]:
    return [replace(check, name=f"{phase}:{check.name}") for check in checks]


def deep_merge(base: dict[str, Any], override: dict[str, Any]) -> dict[str, Any]:
    merged = dict(base)
    for key, value in override.items():
        if isinstance(value, dict) and isinstance(merged.get(key), dict):
            merged[key] = deep_merge(merged[key], value)
        else:
            merged[key] = value
    return merged


def assisted_execution_checks(
    status: int | None,
    response_json: dict[str, Any] | None,
    configuration_error: str,
) -> list[CheckResult]:
    return [
        CheckResult("auto_confirm_config", not configuration_error, configuration_error or "configured"),
        CheckResult("assisted_http_status", status == 200, f"expected=200, actual={status}"),
        CheckResult(
            "assisted_final_execution",
            is_final_execution(status, response_json),
            "final response must contain generatedDsl.query and must not require confirmation",
        ),
    ]


def is_final_execution(status: int | None, response_json: dict[str, Any] | None) -> bool:
    if status != 200 or response_json is None or get_path(response_json, "needsConfirmation") is True:
        return False
    generated_dsl = response_json.get("generatedDsl")
    return isinstance(generated_dsl, dict) and isinstance(generated_dsl.get("query"), dict)


def assign_metric_groups(case: EvaluationCase, checks: list[CheckResult]) -> list[CheckResult]:
    expected_text = json.dumps(case.expected, sort_keys=True).lower()
    aggregation_case = any(token in expected_text for token in (
        "terms_aggregation", "time_aggregation", "date_histogram", "top_values"
    ))

    def group_for(name: str) -> str:
        lowered = name.lower()
        if name == "latency_budget":
            return "Performance"
        if any(token in lowered for token in (
            "confirmation", "warning", "diagnostic", "response_contains",
            "dsl_not_contains", "path_not_exists", "field_not_exists",
        )):
            return "Safety / Guardrail"
        if any(token in lowered for token in (
            "execution_evidence", "total_count", "execution_payload",
            "non_empty:results", "non_empty:aggregations",
        )):
            return "Result Quality"
        if any(token in lowered for token in (
            "generateddsl", "dsl_", "selectedtemplate", "selected_template", "charttype", "chart_type"
        )):
            return "Aggregation Correctness" if aggregation_case else "DSL Correctness"
        return "Result Quality"

    return [replace(check, metric_group=group_for(check.name)) for check in checks]


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
        checks.append(any_of_check(
            expected["anyOf"],
            response_json,
            inherited_allow_zero_results=expected.get("allowZeroResults", False),
        ))

    if "max_latency_ms" in thresholds:
        max_latency_ms = float(thresholds["max_latency_ms"])
        checks.append(CheckResult("latency_budget", latency_ms <= max_latency_ms, f"budget={max_latency_ms:.1f} ms, actual={latency_ms:.1f} ms"))

    return checks


def any_of_check(
    branches: list[dict[str, Any]],
    response_json: dict[str, Any],
    inherited_allow_zero_results: bool = False,
) -> CheckResult:
    failures: list[str] = []
    for index, branch in enumerate(branches, start=1):
        branch_checks = evaluate_branch(branch, response_json, inherited_allow_zero_results)
        if all(check.passed for check in branch_checks):
            return CheckResult("any_of", True, f"matched branch {index}")
        failed = ", ".join(f"{check.name}: {check.detail}" for check in branch_checks if not check.passed)
        failures.append(f"branch {index} failed [{failed}]")
    return CheckResult("any_of", False, "; ".join(failures))


def evaluate_branch(
    expected: dict[str, Any],
    response_json: dict[str, Any],
    inherited_allow_zero_results: bool = False,
) -> list[CheckResult]:
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

    allow_zero_results = expected.get("allowZeroResults", inherited_allow_zero_results)
    if expected.get("requiresExecutionEvidence", False) and not allow_zero_results:
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
