# Backend Evaluation Report

- Generated at: `2026-06-29 10:20:30 UTC`
- Backend: `http://localhost:8080`
- Python: `3.12.3`
- Cases: `28`
- Repeat: `1`
- Warmup: `0`
- Variant: `workflow-v2`
- Data snapshot: `soc-events-2026-06-29`
- Provider config: `gemini-2.5-flash`
- Cache regime: `cold`
- Request fingerprint: `9635377dde236b41267a0deec98ee0d1509beaef0b0242d3e30284074c01858e`
- Evaluation fingerprint: `c0be8bb212cc47d6f4596ae363902f6ae834220211685c20e1d2d90f21218169`
- Verdict: **PASS**

## Summary

| Metric | Value |
|---|---:|
| Total runs | 28 |
| Passed | 26 |
| Failed | 2 |
| Pass rate | 92.9% |
| Status error rate | 0.0% |
| Executed runs | 28 |
| Required executions | 28 |
| Execution target met | True |
| Confirmation runs | 23 |
| Confirmations followed | 23 |
| Confirmation rate | 82.1% |
| Cache hits | 0 |
| Cache hit rate | 0.0% |
| Zero-result runs | 16 |
| Zero-result rate | 57.1% |
| Latency min | 62.4 ms |
| Latency median | 343.4 ms |
| Latency p95 | 3241.4 ms |
| Latency max | 4776.3 ms |

## Category Breakdown

| Category | Runs | Pass Rate | Latency p95 |
|---|---:|---:|---:|
| ambiguity | 7 | 100.0% | 369.5 ms |
| api_contract | 4 | 100.0% | 2941.5 ms |
| llm_language | 6 | 83.3% | 346.4 ms |
| perception_prefilter | 2 | 100.0% | 194.3 ms |
| soc_nl2plan | 2 | 100.0% | 4699.6 ms |
| temporal | 7 | 85.7% | 374.4 ms |

## Metric Group Scorecard

| Metric group | Runs | Run pass rate | Checks | Check pass rate | Latency p95 |
|---|---:|---:|---:|---:|---:|
| DSL Correctness | 23 | 95.7% | 100 | 99.0% | 3242.3 ms |
| Aggregation Correctness | 5 | 80.0% | 33 | 97.0% | 381.0 ms |
| Result Quality | 28 | 100.0% | 219 | 100.0% | 3241.4 ms |
| Safety / Guardrail | 23 | 100.0% | 85 | 100.0% | 1237.3 ms |

## Template Distribution

| Template | Runs |
|---|---:|
| SIMPLE_SEARCH | 17 |
| TERMS_AGGREGATION | 6 |
| TIME_AGGREGATION | 5 |

## Case Results

| Case | Category | Tags | Iteration | Status | Template | Confirm | Cache | Total | Rows | Aggs | Latency | Result | Failed Checks |
|---|---|---|---:|---:|---|---:|---:|---:|---:|---:|---:|---|---|
| soc-001 | soc_nl2plan | - | 1 | 200 | SIMPLE_SEARCH | False | False | 3142 | 50 | 0 | 4776.3 ms | PASS | - |
| soc-002 | soc_nl2plan | - | 1 | 200 | SIMPLE_SEARCH | False | False | 0 | 0 | 0 | 3242.6 ms | PASS | - |
| soc-003 | api_contract | - | 1 | 200 | SIMPLE_SEARCH | True | False | 100001 | 50 | 0 | 3239.1 ms | PASS | - |
| soc-004 | api_contract | - | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 1254.8 ms | PASS | - |
| soc-005 | api_contract | - | 1 | 200 | SIMPLE_SEARCH | True | False | 1 | 0 | 0 | 1054.9 ms | PASS | - |
| soc-006 | api_contract | - | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 1080.3 ms | PASS | - |
| amb-001 | ambiguity | - | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 342.9 ms | PASS | - |
| amb-002 | ambiguity | - | 1 | 200 | TERMS_AGGREGATION | False | False | 100001 | 500 | 1 | 204.0 ms | PASS | - |
| amb-003 | ambiguity | - | 1 | 200 | TERMS_AGGREGATION | True | False | 100001 | 50 | 8 | 329.8 ms | PASS | - |
| amb-004 | ambiguity | - | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 345.8 ms | PASS | - |
| amb-005 | ambiguity | - | 1 | 200 | TIME_AGGREGATION | True | False | 100001 | 50 | 41 | 364.3 ms | PASS | - |
| amb-006 | perception_prefilter | - | 1 | 200 | TERMS_AGGREGATION | False | False | 100001 | 100 | 5 | 198.2 ms | PASS | - |
| amb-007 | perception_prefilter | - | 1 | 200 | TERMS_AGGREGATION | True | False | 100001 | 100 | 10 | 121.0 ms | PASS | - |
| amb-008 | ambiguity | - | 1 | 200 | TERMS_AGGREGATION | True | False | 100001 | 50 | 8 | 335.9 ms | PASS | - |
| amb-009 | ambiguity | - | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 371.7 ms | PASS | - |
| llm-001 | llm_language | - | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 333.9 ms | PASS | - |
| llm-002 | llm_language | - | 1 | 200 | TERMS_AGGREGATION | False | False | 100001 | 100 | 1 | 62.4 ms | FAIL | `final:dsl_any_contains:['error', 'failure']` no alternative fragment found |
| llm-003 | llm_language | - | 1 | 200 | TIME_AGGREGATION | True | False | 0 | 0 | 0 | 324.7 ms | PASS | - |
| llm-004 | llm_language | - | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 314.4 ms | PASS | - |
| llm-005 | llm_language | - | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 303.8 ms | PASS | - |
| llm-006 | llm_language | - | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 350.6 ms | PASS | - |
| tmp-001 | temporal | - | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 315.1 ms | PASS | - |
| tmp-002 | temporal | - | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 343.4 ms | PASS | - |
| tmp-003 | temporal | - | 1 | 200 | TIME_AGGREGATION | True | False | 980 | 100 | 359 | 385.2 ms | PASS | - |
| tmp-004 | temporal | - | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 347.1 ms | PASS | - |
| tmp-005 | temporal | - | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 343.3 ms | PASS | - |
| tmp-006 | temporal | - | 1 | 200 | TIME_AGGREGATION | True | False | 7878 | 100 | 360 | 349.2 ms | FAIL | `final:dsl_contains:"fixed_interval":"1d"` fragment not found |
| tmp-007 | temporal | - | 1 | 200 | TIME_AGGREGATION | True | False | 628 | 50 | 4 | 309.3 ms | PASS | - |
- `evaluation\cases\ablation\ab_execution_cases.jsonl`
