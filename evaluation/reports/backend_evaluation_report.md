# Backend Evaluation Report

- Generated at: `2026-06-21 11:49:22 UTC`
- Backend: `http://localhost:8080`
- Python: `3.12.3`
- Cases: `33`
- Repeat: `1`
- Warmup: `0`
- Verdict: **PASS**

## Summary

| Metric | Value |
|---|---:|
| Total runs | 33 |
| Passed | 33 |
| Failed | 0 |
| Pass rate | 100.0% |
| Status error rate | 0.0% |
| Executed runs | 3 |
| Confirmation runs | 30 |
| Confirmation rate | 90.9% |
| Cache hits | 3 |
| Cache hit rate | 9.1% |
| Zero-result runs | 0 |
| Zero-result rate | 0.0% |
| Latency min | 31.0 ms |
| Latency median | 298.2 ms |
| Latency p95 | 1012.3 ms |
| Latency max | 1218.0 ms |

## Category Breakdown

| Category | Runs | Pass Rate | Latency p95 |
|---|---:|---:|---:|
| ambiguity | 7 | 100.0% | 751.3 ms |
| api_contract | 4 | 100.0% | 454.1 ms |
| llm_language | 6 | 100.0% | 320.0 ms |
| perception_prefilter | 2 | 100.0% | 69.4 ms |
| residual | 5 | 100.0% | 317.1 ms |
| soc_nl2plan | 2 | 100.0% | 1213.4 ms |
| temporal | 7 | 100.0% | 303.4 ms |

## Template Distribution

| Template | Runs |
|---|---:|
| SIMPLE_SEARCH | 23 |
| TERMS_AGGREGATION | 6 |
| TIME_AGGREGATION | 4 |

## Case Results

| Case | Category | Tags | Iteration | Status | Template | Confirm | Cache | Total | Rows | Aggs | Latency | Result | Failed Checks |
|---|---|---|---:|---:|---|---:|---:|---:|---:|---:|---:|---|---|
| soc-001 | soc_nl2plan | - | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 1218.0 ms | PASS | - |
| soc-002 | soc_nl2plan | - | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 1126.4 ms | PASS | - |
| soc-003 | api_contract | - | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 476.6 ms | PASS | - |
| soc-004 | api_contract | explicit_filters, review | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 294.6 ms | PASS | - |
| soc-005 | api_contract | pagination, exact_filter, review | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 326.5 ms | PASS | - |
| soc-006 | api_contract | ip, exact_filter, review | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 307.3 ms | PASS | - |
| tmp-001 | temporal | - | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 284.7 ms | PASS | - |
| tmp-002 | temporal | - | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 278.8 ms | PASS | - |
| tmp-003 | temporal | - | 1 | 200 | TIME_AGGREGATION | True | False | 0 | 0 | 0 | 296.4 ms | PASS | - |
| tmp-004 | temporal | explicit_time, open_range | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 305.3 ms | PASS | - |
| tmp-005 | temporal | explicit_time, pagination | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 287.3 ms | PASS | - |
| tmp-006 | temporal | time_aggregation, explicit_time | 1 | 200 | TIME_AGGREGATION | True | False | 0 | 0 | 0 | 288.4 ms | PASS | - |
| tmp-007 | temporal | quarter, time_aggregation, regression | 1 | 200 | TIME_AGGREGATION | True | False | 0 | 0 | 0 | 298.8 ms | PASS | - |
| res-001 | residual | - | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 284.5 ms | PASS | - |
| res-002 | residual | - | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 302.2 ms | PASS | - |
| res-003 | residual | free_text, message_only | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 320.8 ms | PASS | - |
| res-004 | residual | explicit_filter, free_text | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 282.6 ms | PASS | - |
| res-005 | residual | noise_words | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 298.1 ms | PASS | - |
| amb-001 | ambiguity | - | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 298.2 ms | PASS | - |
| amb-002 | ambiguity | - | 1 | 200 | TERMS_AGGREGATION | False | True | 200000 | 500 | 10 | 116.3 ms | PASS | - |
| amb-003 | ambiguity | - | 1 | 200 | TERMS_AGGREGATION | True | False | 0 | 0 | 0 | 289.6 ms | PASS | - |
| amb-004 | ambiguity | - | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 319.7 ms | PASS | - |
| amb-005 | ambiguity | - | 1 | 200 | TIME_AGGREGATION | True | False | 0 | 0 | 0 | 273.8 ms | PASS | - |
| amb-006 | perception_prefilter | prefilter, terms_aggregation | 1 | 200 | TERMS_AGGREGATION | False | True | 200000 | 100 | 5 | 71.4 ms | PASS | - |
| amb-007 | perception_prefilter | prefilter, terms_aggregation, top_n_required | 1 | 200 | TERMS_AGGREGATION | True | False | 0 | 0 | 0 | 31.0 ms | PASS | - |
| amb-008 | ambiguity | missing_group_by | 1 | 200 | TERMS_AGGREGATION | True | False | 0 | 0 | 0 | 307.1 ms | PASS | - |
| amb-009 | ambiguity | follow_up | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 936.2 ms | PASS | - |
| llm-001 | llm_language | vietnamese, auth, temporal | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 305.7 ms | PASS | - |
| llm-002 | llm_language | mixed_language, statistics | 1 | 200 | TERMS_AGGREGATION | False | True | 200000 | 100 | 3 | 151.2 ms | PASS | - |
| llm-003 | llm_language | time_bucket, trend | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 261.8 ms | PASS | - |
| llm-004 | llm_language | command_noise, free_text | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 306.5 ms | PASS | - |
| llm-005 | llm_language | mitre_like, ambiguous_attack | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 305.5 ms | PASS | - |
| llm-006 | llm_language | explicit_filter_priority | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 324.5 ms | PASS | - |
- `evaluation\cases\soc_nl2plan_v1.jsonl`
- `evaluation\cases\temporal_cases.jsonl`
- `evaluation\cases\residual_cases.jsonl`
- `evaluation\cases\ambiguity_cases.jsonl`
- `evaluation\cases\llm_language_cases.jsonl`
