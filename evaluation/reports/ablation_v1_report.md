# Backend Evaluation Report

- Generated at: `2026-06-26 15:40:57 UTC`
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
| Passed | 28 |
| Failed | 0 |
| Pass rate | 100.0% |
| Status error rate | 0.0% |
| Executed runs | 3 |
| Confirmation runs | 25 |
| Confirmation rate | 89.3% |
| Cache hits | 3 |
| Cache hit rate | 10.7% |
| Zero-result runs | 0 |
| Zero-result rate | 0.0% |
| Latency min | 6.5 ms |
| Latency median | 288.0 ms |
| Latency p95 | 939.2 ms |
| Latency max | 1587.5 ms |

## Category Breakdown

| Category | Runs | Pass Rate | Latency p95 |
|---|---:|---:|---:|
| ambiguity | 7 | 100.0% | 387.5 ms |
| api_contract | 4 | 100.0% | 824.1 ms |
| llm_language | 6 | 100.0% | 383.2 ms |
| perception_prefilter | 2 | 100.0% | 64.7 ms |
| soc_nl2plan | 2 | 100.0% | 1556.1 ms |
| temporal | 7 | 100.0% | 301.8 ms |

## Template Distribution

| Template | Runs |
|---|---:|
| SIMPLE_SEARCH | 18 |
| TERMS_AGGREGATION | 6 |
| TIME_AGGREGATION | 4 |

## Case Results

| Case | Category | Tags | Iteration | Status | Template | Confirm | Cache | Total | Rows | Aggs | Latency | Result | Failed Checks |
|---|---|---|---:|---:|---|---:|---:|---:|---:|---:|---:|---|---|
| soc-001 | soc_nl2plan | - | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 1587.5 ms | PASS | - |
| soc-002 | soc_nl2plan | - | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 958.8 ms | PASS | - |
| soc-003 | api_contract | - | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 902.8 ms | PASS | - |
| soc-004 | api_contract | explicit_filters, review | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 378.4 ms | PASS | - |
| soc-005 | api_contract | pagination, exact_filter, review | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 273.4 ms | PASS | - |
| soc-006 | api_contract | ip, exact_filter, review | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 278.8 ms | PASS | - |
| amb-001 | ambiguity | - | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 283.7 ms | PASS | - |
| amb-002 | ambiguity | - | 1 | 200 | TERMS_AGGREGATION | False | True | 100001 | 500 | 1 | 108.0 ms | PASS | - |
| amb-003 | ambiguity | - | 1 | 200 | TERMS_AGGREGATION | True | False | 0 | 0 | 0 | 328.9 ms | PASS | - |
| amb-004 | ambiguity | - | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 412.6 ms | PASS | - |
| amb-005 | ambiguity | - | 1 | 200 | TIME_AGGREGATION | True | False | 0 | 0 | 0 | 259.6 ms | PASS | - |
| amb-006 | perception_prefilter | prefilter, terms_aggregation | 1 | 200 | TERMS_AGGREGATION | False | True | 100001 | 100 | 5 | 67.8 ms | PASS | - |
| amb-007 | perception_prefilter | prefilter, terms_aggregation, top_n_required | 1 | 200 | TERMS_AGGREGATION | True | False | 0 | 0 | 0 | 6.5 ms | PASS | - |
| amb-008 | ambiguity | missing_group_by | 1 | 200 | TERMS_AGGREGATION | True | False | 0 | 0 | 0 | 288.5 ms | PASS | - |
| amb-009 | ambiguity | follow_up | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 291.6 ms | PASS | - |
| llm-001 | llm_language | vietnamese, auth, temporal | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 275.8 ms | PASS | - |
| llm-002 | llm_language | mixed_language, statistics | 1 | 200 | TERMS_AGGREGATION | False | True | 100001 | 100 | 1 | 56.6 ms | PASS | - |
| llm-003 | llm_language | time_bucket, trend | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 288.1 ms | PASS | - |
| llm-004 | llm_language | command_noise, free_text | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 273.0 ms | PASS | - |
| llm-005 | llm_language | mitre_like, ambiguous_attack | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 410.7 ms | PASS | - |
| llm-006 | llm_language | explicit_filter_priority | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 300.4 ms | PASS | - |
| tmp-001 | temporal | - | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 286.1 ms | PASS | - |
| tmp-002 | temporal | - | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 293.5 ms | PASS | - |
| tmp-003 | temporal | - | 1 | 200 | TIME_AGGREGATION | True | False | 0 | 0 | 0 | 262.2 ms | PASS | - |
| tmp-004 | temporal | explicit_time, open_range | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 267.5 ms | PASS | - |
| tmp-005 | temporal | explicit_time, pagination | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 287.9 ms | PASS | - |
| tmp-006 | temporal | time_aggregation, explicit_time | 1 | 200 | TIME_AGGREGATION | True | False | 0 | 0 | 0 | 305.3 ms | PASS | - |
| tmp-007 | temporal | quarter, time_aggregation, regression | 1 | 200 | TIME_AGGREGATION | True | False | 0 | 0 | 0 | 292.9 ms | PASS | - |
- `evaluation\cases\ablation\workflow_comparison_cases.jsonl`
