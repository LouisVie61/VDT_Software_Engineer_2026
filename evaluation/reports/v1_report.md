# Backend Evaluation Report

- Generated at: `2026-07-01 13:17:04 UTC`
- Backend: `http://localhost:8080`
- Python: `3.12.3`
- Cases: `28`
- Repeat: `1`
- Warmup: `0`
- Variant: `llm-only-v1`
- Data snapshot: `soc-events-fixed`
- Provider config: `gemini-2.5-flash`
- Cache regime: `cold`
- Request fingerprint: `4b78e1053d2e30d997558e7079cd19e5b6297824320b202a8de5d272657dda8d`
- Evaluation fingerprint: `84a3146ecb2accbd60ad2aa669e6ea2ed310863bd5061e18f2de8bffc8b143c3`
- Verdict: **FAIL**

## Summary

| Metric | Value |
|---|---:|
| Total runs | 28 |
| Passed | 21 |
| Failed | 7 |
| Pass rate | 75.0% |
| Status error rate | 3.6% |
| Executed runs | 27 |
| Required executions | 28 |
| Execution target met | False |
| Confirmation runs | 0 |
| Confirmations followed | 0 |
| Confirmation rate | 0.0% |
| Cache hits | 0 |
| Cache hit rate | 0.0% |
| Zero-result runs | 11 |
| Zero-result rate | 40.7% |
| Latency min | 7504.8 ms |
| Latency median | 54967.5 ms |
| Latency p95 | 77956.8 ms |
| Latency max | 82655.6 ms |

## Category Breakdown

| Category | Runs | Pass Rate | Latency p95 |
|---|---:|---:|---:|
| ambiguity | 7 | 57.1% | 76664.6 ms |
| api_contract | 4 | 100.0% | 25065.0 ms |
| llm_language | 6 | 66.7% | 76819.2 ms |
| perception_prefilter | 2 | 100.0% | 76608.8 ms |
| soc_nl2plan | 2 | 100.0% | 12564.9 ms |
| temporal | 7 | 71.4% | 78463.3 ms |

## Metric Group Scorecard

| Metric group | Runs | Run pass rate | Checks | Check pass rate | Latency p95 |
|---|---:|---:|---:|---:|---:|
| DSL Correctness | 17 | 94.1% | 63 | 98.4% | 78989.0 ms |
| Aggregation Correctness | 11 | 45.5% | 59 | 71.2% | 75208.9 ms |
| Result Quality | 28 | 82.1% | 123 | 92.7% | 77956.8 ms |
| Safety / Guardrail | 6 | 100.0% | 16 | 100.0% | 77989.8 ms |

## Template Distribution

| Template | Runs |
|---|---:|
| none | 28 |

## Case Results

| Case | Category | Tags | Iteration | Status | Template | Confirm | Cache | Total | Rows | Aggs | Latency | Result | Failed Checks |
|---|---|---|---:|---:|---|---:|---:|---:|---:|---:|---:|---|---|
| soc-001 | soc_nl2plan | - | 1 | 200 | - | None | None | 3142 | 50 | 0 | 12797.1 ms | PASS | - |
| soc-002 | soc_nl2plan | - | 1 | 200 | - | None | None | 2 | 2 | 0 | 8153.3 ms | PASS | - |
| soc-003 | api_contract | - | 1 | 200 | - | None | None | 10000 | 50 | 0 | 11392.1 ms | PASS | - |
| soc-004 | api_contract | - | 1 | 200 | - | None | None | 2618 | 30 | 0 | 12730.3 ms | PASS | - |
| soc-005 | api_contract | - | 1 | 200 | - | None | None | 2 | 0 | 0 | 27241.8 ms | PASS | - |
| soc-006 | api_contract | - | 1 | 200 | - | None | None | 2 | 2 | 0 | 7504.8 ms | PASS | - |
| amb-001 | ambiguity | - | 1 | 200 | - | None | None | 0 | 0 | 0 | 9257.1 ms | PASS | - |
| amb-002 | ambiguity | - | 1 | 200 | - | None | None | 10000 | 500 | 10 | 33206.1 ms | PASS | - |
| amb-003 | ambiguity | - | 1 | 200 | - | None | None | 0 | 0 | 0 | 35253.4 ms | FAIL | `dsl_contains:"terms"` fragment not found<br>`dsl_contains:"field":"event_type"` fragment not found<br>`dsl_contains:"size":10` fragment not found<br>`any_of` branch 1 failed [generatedDsl_path_exists:aggs: missing path]; branch 2 failed [generatedDsl_path_exists:aggregations: missing path] |
| amb-004 | ambiguity | - | 1 | 200 | - | None | None | 0 | 0 | 0 | 54934.8 ms | PASS | - |
| amb-005 | ambiguity | - | 1 | 200 | - | None | None | 0 | 0 | 0 | 56749.5 ms | FAIL | `dsl_contains:"date_histogram"` fragment not found<br>`dsl_contains:"calendar_interval":"quarter"` fragment not found<br>`any_of` branch 1 failed [generatedDsl_path_exists:aggs: missing path]; branch 2 failed [generatedDsl_path_exists:aggregations: missing path] |
| amb-006 | perception_prefilter | - | 1 | 200 | - | None | None | 10000 | 100 | 5 | 68456.7 ms | PASS | - |
| amb-007 | perception_prefilter | - | 1 | 200 | - | None | None | 10000 | 100 | 23 | 77037.9 ms | PASS | - |
| amb-008 | ambiguity | - | 1 | 200 | - | None | None | 0 | 0 | 0 | 73379.9 ms | FAIL | `dsl_contains:"terms"` fragment not found<br>`dsl_contains:"field":"event_type"` fragment not found<br>`dsl_contains:"size":10` fragment not found<br>`any_of` branch 1 failed [generatedDsl_path_exists:aggs: missing path]; branch 2 failed [generatedDsl_path_exists:aggregations: missing path] |
| amb-009 | ambiguity | - | 1 | 200 | - | None | None | 10000 | 50 | 0 | 78072.3 ms | PASS | - |
| llm-001 | llm_language | - | 1 | 200 | - | None | None | 0 | 0 | 0 | 74049.8 ms | PASS | - |
| llm-002 | llm_language | - | 1 | 200 | - | None | None | 10000 | 100 | 3 | 65606.9 ms | FAIL | `dsl_any_contains:['error', 'failure']` no alternative fragment found |
| llm-003 | llm_language | - | 1 | 200 | - | None | None | 2475 | 100 | 0 | 52816.9 ms | FAIL | `dsl_contains:"date_histogram"` fragment not found<br>`dsl_any_contains:['"fixed_interval":"1d"', '"calendar_interval":"1d"', '"calendar_interval":"day"']` no alternative fragment found<br>`any_of` branch 1 failed [generatedDsl_path_exists:aggs: missing path]; branch 2 failed [generatedDsl_path_exists:aggregations: missing path] |
| llm-004 | llm_language | - | 1 | 200 | - | None | None | 0 | 0 | 0 | 66892.7 ms | PASS | - |
| llm-005 | llm_language | - | 1 | 200 | - | None | None | 0 | 0 | 0 | 77742.4 ms | PASS | - |
| llm-006 | llm_language | - | 1 | 200 | - | None | None | 0 | 0 | 0 | 59213.2 ms | PASS | - |
| tmp-001 | temporal | - | 1 | 200 | - | None | None | 7878 | 50 | 0 | 47294.9 ms | PASS | - |
| tmp-002 | temporal | - | 1 | 200 | - | None | None | 0 | 0 | 0 | 57153.2 ms | PASS | - |
| tmp-003 | temporal | - | 1 | 200 | - | None | None | 980 | 100 | 359 | 68681.2 ms | PASS | - |
| tmp-004 | temporal | - | 1 | 200 | - | None | None | 10000 | 50 | 0 | 55000.1 ms | FAIL | `dsl_contains:endpoint` fragment not found |
| tmp-005 | temporal | - | 1 | 200 | - | None | None | 1937 | 15 | 0 | 82655.6 ms | PASS | - |
| tmp-006 | temporal | - | 1 | 200 | - | None | None | 7878 | 100 | 15 | 39019.9 ms | PASS | - |
| tmp-007 | temporal | - | 1 | 400 | - | None | None | None | 0 | 0 | 36877.3 ms | FAIL | `http_status` expected=200, actual=400<br>`field:generatedDsl` missing field<br>`field:totalCount` missing field<br>`field:results` missing field<br>`field:aggregations` missing field<br>`generatedDsl_path_exists:query` missing path<br>`dsl_contains:"date_histogram"` fragment not found<br>`dsl_contains:"calendar_interval":"quarter"` fragment not found<br>`dsl_contains:2023-01-01` fragment not found<br>`dsl_contains:2024-01-01` fragment not found<br>`any_of` branch 1 failed [generatedDsl_path_exists:aggs: missing path]; branch 2 failed [generatedDsl_path_exists:aggregations: missing path]<br>HTTP Error 400:  |
- `evaluation\cases\llm_cases.jsonl`
