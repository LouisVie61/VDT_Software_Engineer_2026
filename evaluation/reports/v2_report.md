# Backend Evaluation Report

- Generated at: `2026-06-27 09:13:04 UTC`
- Backend: `http://localhost:8080`
- Python: `3.12.3`
- Cases: `28`
- Repeat: `1`
- Warmup: `0`
- Verdict: **PASS**

## Summary

| Metric | Value |
|---|---:|
| Total runs | 28 |
| Passed | 26 |
| Failed | 2 |
| Pass rate | 92.9% |
| Status error rate | 0.0% |
| Executed runs | 8 |
| Confirmation runs | 20 |
| Confirmation rate | 71.4% |
| Cache hits | 7 |
| Cache hit rate | 25.0% |
| Zero-result runs | 4 |
| Zero-result rate | 50.0% |
| Latency min | 13.7 ms |
| Latency median | 324.8 ms |
| Latency p95 | 6762.2 ms |
| Latency max | 8159.2 ms |

## Category Breakdown

| Category | Runs | Pass Rate | Latency p95 |
|---|---:|---:|---:|
| ambiguity | 7 | 71.4% | 7768.6 ms |
| api_contract | 4 | 100.0% | 6100.7 ms |
| llm_language | 6 | 100.0% | 322.5 ms |
| perception_prefilter | 2 | 100.0% | 94.2 ms |
| soc_nl2plan | 2 | 100.0% | 3523.8 ms |
| temporal | 7 | 100.0% | 360.1 ms |

## Metric Group Scorecard

| Metric group | Runs | Run pass rate | Checks | Check pass rate | Latency p95 |
|---|---:|---:|---:|---:|---:|
| DSL Correctness | 15 | 100.0% | 61 | 100.0% | 4442.8 ms |
| Aggregation Correctness | 13 | 100.0% | 86 | 100.0% | 7378.0 ms |
| Result Quality | 28 | 100.0% | 431 | 100.0% | 6762.2 ms |
| Safety / Guardrail | 28 | 100.0% | 131 | 100.0% | 6762.2 ms |
| Performance | 28 | 92.9% | 28 | 92.9% | 6762.2 ms |

## Template Distribution

| Template | Runs |
|---|---:|
| SIMPLE_SEARCH | 18 |
| TERMS_AGGREGATION | 6 |
| TIME_AGGREGATION | 4 |

## Case Results

| Case | Category | Tags | Iteration | Status | Template | Confirm | Cache | Total | Rows | Aggs | Latency | Result | Failed Checks |
|---|---|---|---:|---:|---|---:|---:|---:|---:|---:|---:|---|---|
| soc-001 | soc_nl2plan | - | 1 | 200 | SIMPLE_SEARCH | False | True | 3142 | 50 | 0 | 3511.7 ms | PASS | - |
| soc-002 | soc_nl2plan | - | 1 | 200 | SIMPLE_SEARCH | False | True | 0 | 0 | 0 | 3524.5 ms | PASS | - |
| soc-003 | api_contract | - | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 2724.9 ms | PASS | - |
| soc-004 | api_contract | explicit_filters, review | 1 | 200 | SIMPLE_SEARCH | False | False | 0 | 0 | 0 | 6585.6 ms | PASS | - |
| soc-005 | api_contract | pagination, exact_filter, review | 1 | 200 | SIMPLE_SEARCH | False | True | 1 | 0 | 0 | 2997.9 ms | PASS | - |
| soc-006 | api_contract | ip, exact_filter, review | 1 | 200 | SIMPLE_SEARCH | False | True | 0 | 0 | 0 | 3353.5 ms | PASS | - |
| amb-001 | ambiguity | - | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 1107.6 ms | PASS | - |
| amb-002 | ambiguity | - | 1 | 200 | TERMS_AGGREGATION | False | True | 100001 | 500 | 1 | 152.2 ms | PASS | - |
| amb-003 | ambiguity | - | 1 | 200 | TERMS_AGGREGATION | True | False | 0 | 0 | 0 | 6857.3 ms | FAIL | `latency_budget` budget=3000.0 ms, actual=6857.3 ms |
| amb-004 | ambiguity | - | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 328.2 ms | PASS | - |
| amb-005 | ambiguity | - | 1 | 200 | TIME_AGGREGATION | True | False | 0 | 0 | 0 | 8159.2 ms | FAIL | `latency_budget` budget=3000.0 ms, actual=8159.2 ms |
| amb-006 | perception_prefilter | prefilter, terms_aggregation | 1 | 200 | TERMS_AGGREGATION | False | True | 100001 | 100 | 5 | 98.5 ms | PASS | - |
| amb-007 | perception_prefilter | prefilter, terms_aggregation, top_n_required | 1 | 200 | TERMS_AGGREGATION | True | False | 0 | 0 | 0 | 13.7 ms | PASS | - |
| amb-008 | ambiguity | missing_group_by | 1 | 200 | TERMS_AGGREGATION | True | False | 0 | 0 | 0 | 321.5 ms | PASS | - |
| amb-009 | ambiguity | follow_up | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 295.3 ms | PASS | - |
| llm-001 | llm_language | vietnamese, auth, temporal | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 305.6 ms | PASS | - |
| llm-002 | llm_language | mixed_language, statistics | 1 | 200 | TERMS_AGGREGATION | False | True | 100001 | 100 | 1 | 59.6 ms | PASS | - |
| llm-003 | llm_language | time_bucket, trend | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 300.4 ms | PASS | - |
| llm-004 | llm_language | command_noise, free_text | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 293.0 ms | PASS | - |
| llm-005 | llm_language | mitre_like, ambiguous_attack | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 279.6 ms | PASS | - |
| llm-006 | llm_language | explicit_filter_priority | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 328.1 ms | PASS | - |
| tmp-001 | temporal | - | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 361.5 ms | PASS | - |
| tmp-002 | temporal | - | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 280.1 ms | PASS | - |
| tmp-003 | temporal | - | 1 | 200 | TIME_AGGREGATION | True | False | 0 | 0 | 0 | 356.8 ms | PASS | - |
| tmp-004 | temporal | explicit_time, open_range | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 321.0 ms | PASS | - |
| tmp-005 | temporal | explicit_time, pagination | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 349.6 ms | PASS | - |
| tmp-006 | temporal | time_aggregation, explicit_time | 1 | 200 | TIME_AGGREGATION | True | False | 0 | 0 | 0 | 301.5 ms | PASS | - |
| tmp-007 | temporal | quarter, time_aggregation, regression | 1 | 200 | TIME_AGGREGATION | True | False | 0 | 0 | 0 | 278.5 ms | PASS | - |
- `evaluation\cases\ablation\workflow_comparison_cases.jsonl`
