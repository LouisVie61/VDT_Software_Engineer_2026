    # Backend Evaluation Report

    - Generated at: `2026-06-29 13:51:53 UTC`
    - Backend: `http://localhost:8080`
    - Python: `3.12.3`
    - Cases: `28`
    - Repeat: `1`
    - Warmup: `0`
    - Variant: `llm-baseline`
    - Data snapshot: `soc-events-gP5CmfSDQcmg0NgqorwMqA-docs100003`
    - Provider config: `gemini-2.5-flash`
    - Cache regime: `cold`
    - Request fingerprint: `4b78e1053d2e30d997558e7079cd19e5b6297824320b202a8de5d272657dda8d`
    - Evaluation fingerprint: `84a3146ecb2accbd60ad2aa669e6ea2ed310863bd5061e18f2de8bffc8b143c3`
    - Verdict: **FAIL**

    ## Summary

    | Metric | Value |
    |---|---:|
    | Total runs | 28 |
    | Passed | 22 |
    | Failed | 6 |
    | Pass rate | 78.6% |
    | Status error rate | 3.6% |
    | Executed runs | 27 |
    | Required executions | 28 |
    | Execution target met | False |
    | Confirmation runs | 0 |
    | Confirmations followed | 0 |
    | Confirmation rate | 0.0% |
    | Cache hits | 0 |
    | Cache hit rate | 0.0% |
    | Zero-result runs | 12 |
    | Zero-result rate | 44.4% |
    | Latency min | 18444.1 ms |
    | Latency median | 58421.2 ms |
    | Latency p95 | 93643.0 ms |
    | Latency max | 96273.4 ms |

    ## Category Breakdown

    | Category | Runs | Pass Rate | Latency p95 |
    |---|---:|---:|---:|
    | ambiguity | 7 | 57.1% | 75406.5 ms |
    | api_contract | 4 | 100.0% | 75442.4 ms |
    | llm_language | 6 | 83.3% | 92626.3 ms |
    | perception_prefilter | 2 | 100.0% | 91322.4 ms |
    | soc_nl2plan | 2 | 100.0% | 31371.1 ms |
    | temporal | 7 | 71.4% | 93857.1 ms |

    ## Metric Group Scorecard

    | Metric group | Runs | Run pass rate | Checks | Check pass rate | Latency p95 |
    |---|---:|---:|---:|---:|---:|
    | DSL Correctness | 17 | 94.1% | 63 | 98.4% | 94862.0 ms |
    | Aggregation Correctness | 11 | 54.5% | 59 | 72.9% | 90126.9 ms |
    | Result Quality | 28 | 82.1% | 123 | 92.7% | 93643.0 ms |
    | Safety / Guardrail | 6 | 100.0% | 16 | 100.0% | 84834.5 ms |

    ## Template Distribution

    | Template | Runs |
    |---|---:|
    | none | 28 |

    ## Case Results

    | Case | Category | Tags | Iteration | Status | Template | Confirm | Cache | Total | Rows | Aggs | Latency | Result | Failed Checks |
    |---|---|---|---:|---:|---|---:|---:|---:|---:|---:|---:|---|---|
    | soc-001 | soc_nl2plan | - | 1 | 200 | - | None | None | 3142 | 50 | 0 | 18444.1 ms | PASS | - |
    | soc-002 | soc_nl2plan | - | 1 | 200 | - | None | None | 2 | 2 | 0 | 32051.5 ms | PASS | - |
    | soc-003 | api_contract | - | 1 | 200 | - | None | None | 10000 | 50 | 0 | 42671.9 ms | PASS | - |
    | soc-004 | api_contract | - | 1 | 200 | - | None | None | 2618 | 30 | 0 | 42796.2 ms | PASS | - |
    | soc-005 | api_contract | - | 1 | 200 | - | None | None | 2 | 0 | 0 | 79239.6 ms | PASS | - |
    | soc-006 | api_contract | - | 1 | 200 | - | None | None | 0 | 0 | 0 | 53924.9 ms | PASS | - |
    | amb-001 | ambiguity | - | 1 | 200 | - | None | None | 0 | 0 | 0 | 59122.2 ms | PASS | - |
    | amb-002 | ambiguity | - | 1 | 200 | - | None | None | 10000 | 500 | 10 | 64953.0 ms | PASS | - |
    | amb-003 | ambiguity | - | 1 | 200 | - | None | None | 0 | 0 | 0 | 66063.3 ms | FAIL | `dsl_contains:"terms"` fragment not found<br>`dsl_contains:"field":"event_type"` fragment not found<br>`dsl_contains:"size":10` fragment not found<br>`any_of` branch 1 failed [generatedDsl_path_exists:aggs: missing path]; branch 2 failed [generatedDsl_path_exists:aggregations: missing path] |
    | amb-004 | ambiguity | - | 1 | 200 | - | None | None | 0 | 0 | 0 | 49947.2 ms | PASS | - |
    | amb-005 | ambiguity | - | 1 | 200 | - | None | None | 2087 | 50 | 0 | 50816.9 ms | FAIL | `dsl_contains:"date_histogram"` fragment not found<br>`dsl_contains:"calendar_interval":"quarter"` fragment not found<br>`any_of` branch 1 failed [generatedDsl_path_exists:aggs: missing path]; branch 2 failed [generatedDsl_path_exists:aggregations: missing path] |
    | amb-006 | perception_prefilter | - | 1 | 200 | - | None | None | 10000 | 100 | 5 | 77789.3 ms | PASS | - |
    | amb-007 | perception_prefilter | - | 1 | 200 | - | None | None | 10000 | 100 | 23 | 92034.6 ms | PASS | - |
    | amb-008 | ambiguity | - | 1 | 200 | - | None | None | 0 | 0 | 0 | 79410.8 ms | FAIL | `dsl_contains:"terms"` fragment not found<br>`dsl_contains:"field":"event_type"` fragment not found<br>`dsl_contains:"size":10` fragment not found<br>`any_of` branch 1 failed [generatedDsl_path_exists:aggs: missing path]; branch 2 failed [generatedDsl_path_exists:aggregations: missing path] |
    | amb-009 | ambiguity | - | 1 | 200 | - | None | None | 10000 | 50 | 0 | 57720.3 ms | PASS | - |
    | llm-001 | llm_language | - | 1 | 200 | - | None | None | 0 | 0 | 0 | 94509.1 ms | PASS | - |
    | llm-002 | llm_language | - | 1 | 200 | - | None | None | 10000 | 100 | 3 | 50934.3 ms | PASS | - |
    | llm-003 | llm_language | - | 1 | 200 | - | None | None | 0 | 0 | 0 | 67401.5 ms | FAIL | `dsl_contains:"date_histogram"` fragment not found<br>`dsl_any_contains:['"fixed_interval":"1d"', '"calendar_interval":"1d"', '"calendar_interval":"day"']` no alternative fragment found<br>`any_of` branch 1 failed [generatedDsl_path_exists:aggs: missing path]; branch 2 failed [generatedDsl_path_exists:aggregations: missing path] |
    | llm-004 | llm_language | - | 1 | 200 | - | None | None | 0 | 0 | 0 | 78403.9 ms | PASS | - |
    | llm-005 | llm_language | - | 1 | 200 | - | None | None | 0 | 0 | 0 | 86978.1 ms | PASS | - |
    | llm-006 | llm_language | - | 1 | 200 | - | None | None | 0 | 0 | 0 | 33732.9 ms | PASS | - |
    | tmp-001 | temporal | - | 1 | 200 | - | None | None | 7878 | 50 | 0 | 54493.3 ms | PASS | - |
    | tmp-002 | temporal | - | 1 | 200 | - | None | None | 0 | 0 | 0 | 49511.1 ms | PASS | - |
    | tmp-003 | temporal | - | 1 | 200 | - | None | None | 980 | 100 | 359 | 88219.1 ms | PASS | - |
    | tmp-004 | temporal | - | 1 | 200 | - | None | None | 10000 | 50 | 0 | 96273.4 ms | FAIL | `dsl_contains:endpoint` fragment not found |
    | tmp-005 | temporal | - | 1 | 200 | - | None | None | 1937 | 15 | 0 | 43301.7 ms | PASS | - |
    | tmp-006 | temporal | - | 1 | 200 | - | None | None | 7878 | 100 | 15 | 70148.2 ms | PASS | - |
    | tmp-007 | temporal | - | 1 | 400 | - | None | None | None | 0 | 0 | 30487.9 ms | FAIL | `http_status` expected=200, actual=400<br>`field:generatedDsl` missing field<br>`field:totalCount` missing field<br>`field:results` missing field<br>`field:aggregations` missing field<br>`generatedDsl_path_exists:query` missing path<br>`dsl_contains:"date_histogram"` fragment not found<br>`dsl_contains:"calendar_interval":"quarter"` fragment not found<br>`dsl_contains:2023-01-01` fragment not found<br>`dsl_contains:2024-01-01` fragment not found<br>`any_of` branch 1 failed [generatedDsl_path_exists:aggs: missing path]; branch 2 failed [generatedDsl_path_exists:aggregations: missing path]<br>HTTP Error 400:  |
    - `evaluation\cases\ablation\ablation_cases.jsonl`
