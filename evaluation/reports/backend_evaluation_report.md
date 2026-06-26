# Backend Evaluation Report

- Generated at: `2026-06-26 15:32:32 UTC`
- Backend: `http://localhost:8080`
- Python: `3.12.3`
- Cases: `33`
- Repeat: `3`
- Warmup: `1`
- Verdict: **PASS**

## Summary

| Metric | Value |
|---|---:|
| Total runs | 99 |
| Passed | 99 |
| Failed | 0 |
| Pass rate | 100.0% |
| Status error rate | 0.0% |
| Executed runs | 9 |
| Confirmation runs | 90 |
| Confirmation rate | 90.9% |
| Cache hits | 9 |
| Cache hit rate | 9.1% |
| Zero-result runs | 0 |
| Zero-result rate | 0.0% |
| Latency min | 7.0 ms |
| Latency median | 260.1 ms |
| Latency p95 | 342.2 ms |
| Latency max | 924.2 ms |

## Category Breakdown

| Category | Runs | Pass Rate | Latency p95 |
|---|---:|---:|---:|
| ambiguity | 21 | 100.0% | 409.2 ms |
| api_contract | 12 | 100.0% | 575.1 ms |
| llm_language | 18 | 100.0% | 326.3 ms |
| perception_prefilter | 6 | 100.0% | 75.6 ms |
| residual | 15 | 100.0% | 291.0 ms |
| soc_nl2plan | 6 | 100.0% | 318.2 ms |
| temporal | 21 | 100.0% | 306.8 ms |

## Template Distribution

| Template | Runs |
|---|---:|
| SIMPLE_SEARCH | 69 |
| TERMS_AGGREGATION | 18 |
| TIME_AGGREGATION | 12 |

## Case Results

| Case | Category | Tags | Iteration | Status | Template | Confirm | Cache | Total | Rows | Aggs | Latency | Result | Failed Checks |
|---|---|---|---:|---:|---|---:|---:|---:|---:|---:|---:|---|---|
| soc-001 | soc_nl2plan | - | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 276.9 ms | PASS | - |
| soc-002 | soc_nl2plan | - | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 255.8 ms | PASS | - |
| soc-003 | api_contract | - | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 263.2 ms | PASS | - |
| soc-004 | api_contract | explicit_filters, review | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 272.2 ms | PASS | - |
| soc-005 | api_contract | pagination, exact_filter, review | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 265.2 ms | PASS | - |
| soc-006 | api_contract | ip, exact_filter, review | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 271.1 ms | PASS | - |
| amb-001 | ambiguity | - | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 277.8 ms | PASS | - |
| amb-002 | ambiguity | - | 1 | 200 | TERMS_AGGREGATION | False | True | 100001 | 500 | 1 | 96.0 ms | PASS | - |
| amb-003 | ambiguity | - | 1 | 200 | TERMS_AGGREGATION | True | False | 0 | 0 | 0 | 275.1 ms | PASS | - |
| amb-004 | ambiguity | - | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 409.2 ms | PASS | - |
| amb-005 | ambiguity | - | 1 | 200 | TIME_AGGREGATION | True | False | 0 | 0 | 0 | 352.5 ms | PASS | - |
| amb-006 | perception_prefilter | prefilter, terms_aggregation | 1 | 200 | TERMS_AGGREGATION | False | True | 100001 | 100 | 5 | 75.0 ms | PASS | - |
| amb-007 | perception_prefilter | prefilter, terms_aggregation, top_n_required | 1 | 200 | TERMS_AGGREGATION | True | False | 0 | 0 | 0 | 30.6 ms | PASS | - |
| amb-008 | ambiguity | missing_group_by | 1 | 200 | TERMS_AGGREGATION | True | False | 0 | 0 | 0 | 440.0 ms | PASS | - |
| amb-009 | ambiguity | follow_up | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 262.2 ms | PASS | - |
| llm-001 | llm_language | vietnamese, auth, temporal | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 276.8 ms | PASS | - |
| llm-002 | llm_language | mixed_language, statistics | 1 | 200 | TERMS_AGGREGATION | False | True | 100001 | 100 | 1 | 57.2 ms | PASS | - |
| llm-003 | llm_language | time_bucket, trend | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 276.3 ms | PASS | - |
| llm-004 | llm_language | command_noise, free_text | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 260.1 ms | PASS | - |
| llm-005 | llm_language | mitre_like, ambiguous_attack | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 316.7 ms | PASS | - |
| llm-006 | llm_language | explicit_filter_priority | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 380.4 ms | PASS | - |
| tmp-001 | temporal | - | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 276.3 ms | PASS | - |
| tmp-002 | temporal | - | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 266.3 ms | PASS | - |
| tmp-003 | temporal | - | 1 | 200 | TIME_AGGREGATION | True | False | 0 | 0 | 0 | 259.9 ms | PASS | - |
| tmp-004 | temporal | explicit_time, open_range | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 341.1 ms | PASS | - |
| tmp-005 | temporal | explicit_time, pagination | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 255.3 ms | PASS | - |
| tmp-006 | temporal | time_aggregation, explicit_time | 1 | 200 | TIME_AGGREGATION | True | False | 0 | 0 | 0 | 243.3 ms | PASS | - |
| tmp-007 | temporal | quarter, time_aggregation, regression | 1 | 200 | TIME_AGGREGATION | True | False | 0 | 0 | 0 | 306.8 ms | PASS | - |
| res-001 | residual | - | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 275.0 ms | PASS | - |
| res-002 | residual | - | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 285.1 ms | PASS | - |
| res-003 | residual | free_text, message_only | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 260.9 ms | PASS | - |
| res-004 | residual | explicit_filter, free_text | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 299.7 ms | PASS | - |
| res-005 | residual | noise_words | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 250.0 ms | PASS | - |
| soc-001 | soc_nl2plan | - | 2 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 332.0 ms | PASS | - |
| soc-002 | soc_nl2plan | - | 2 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 262.1 ms | PASS | - |
| soc-003 | api_contract | - | 2 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 244.6 ms | PASS | - |
| soc-004 | api_contract | explicit_filters, review | 2 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 239.5 ms | PASS | - |
| soc-005 | api_contract | pagination, exact_filter, review | 2 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 261.4 ms | PASS | - |
| soc-006 | api_contract | ip, exact_filter, review | 2 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 289.4 ms | PASS | - |
| amb-001 | ambiguity | - | 2 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 234.3 ms | PASS | - |
| amb-002 | ambiguity | - | 2 | 200 | TERMS_AGGREGATION | False | True | 100001 | 500 | 1 | 81.1 ms | PASS | - |
| amb-003 | ambiguity | - | 2 | 200 | TERMS_AGGREGATION | True | False | 0 | 0 | 0 | 269.1 ms | PASS | - |
| amb-004 | ambiguity | - | 2 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 252.5 ms | PASS | - |
| amb-005 | ambiguity | - | 2 | 200 | TIME_AGGREGATION | True | False | 0 | 0 | 0 | 262.1 ms | PASS | - |
| amb-006 | perception_prefilter | prefilter, terms_aggregation | 2 | 200 | TERMS_AGGREGATION | False | True | 100001 | 100 | 5 | 75.8 ms | PASS | - |
| amb-007 | perception_prefilter | prefilter, terms_aggregation, top_n_required | 2 | 200 | TERMS_AGGREGATION | True | False | 0 | 0 | 0 | 17.7 ms | PASS | - |
| amb-008 | ambiguity | missing_group_by | 2 | 200 | TERMS_AGGREGATION | True | False | 0 | 0 | 0 | 271.3 ms | PASS | - |
| amb-009 | ambiguity | follow_up | 2 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 258.3 ms | PASS | - |
| llm-001 | llm_language | vietnamese, auth, temporal | 2 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 252.4 ms | PASS | - |
| llm-002 | llm_language | mixed_language, statistics | 2 | 200 | TERMS_AGGREGATION | False | True | 100001 | 100 | 1 | 55.5 ms | PASS | - |
| llm-003 | llm_language | time_bucket, trend | 2 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 261.9 ms | PASS | - |
| llm-004 | llm_language | command_noise, free_text | 2 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 248.5 ms | PASS | - |
| llm-005 | llm_language | mitre_like, ambiguous_attack | 2 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 263.3 ms | PASS | - |
| llm-006 | llm_language | explicit_filter_priority | 2 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 256.0 ms | PASS | - |
| tmp-001 | temporal | - | 2 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 247.6 ms | PASS | - |
| tmp-002 | temporal | - | 2 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 236.8 ms | PASS | - |
| tmp-003 | temporal | - | 2 | 200 | TIME_AGGREGATION | True | False | 0 | 0 | 0 | 251.3 ms | PASS | - |
| tmp-004 | temporal | explicit_time, open_range | 2 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 257.6 ms | PASS | - |
| tmp-005 | temporal | explicit_time, pagination | 2 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 262.4 ms | PASS | - |
| tmp-006 | temporal | time_aggregation, explicit_time | 2 | 200 | TIME_AGGREGATION | True | False | 0 | 0 | 0 | 237.7 ms | PASS | - |
| tmp-007 | temporal | quarter, time_aggregation, regression | 2 | 200 | TIME_AGGREGATION | True | False | 0 | 0 | 0 | 251.9 ms | PASS | - |
| res-001 | residual | - | 2 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 239.9 ms | PASS | - |
| res-002 | residual | - | 2 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 254.3 ms | PASS | - |
| res-003 | residual | free_text, message_only | 2 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 287.3 ms | PASS | - |
| res-004 | residual | explicit_filter, free_text | 2 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 278.8 ms | PASS | - |
| res-005 | residual | noise_words | 2 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 237.5 ms | PASS | - |
| soc-001 | soc_nl2plan | - | 3 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 266.4 ms | PASS | - |
| soc-002 | soc_nl2plan | - | 3 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 270.4 ms | PASS | - |
| soc-003 | api_contract | - | 3 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 250.5 ms | PASS | - |
| soc-004 | api_contract | explicit_filters, review | 3 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 252.3 ms | PASS | - |
| soc-005 | api_contract | pagination, exact_filter, review | 3 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 924.2 ms | PASS | - |
| soc-006 | api_contract | ip, exact_filter, review | 3 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 247.0 ms | PASS | - |
| amb-001 | ambiguity | - | 3 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 240.1 ms | PASS | - |
| amb-002 | ambiguity | - | 3 | 200 | TERMS_AGGREGATION | False | True | 100001 | 500 | 1 | 142.8 ms | PASS | - |
| amb-003 | ambiguity | - | 3 | 200 | TERMS_AGGREGATION | True | False | 0 | 0 | 0 | 268.8 ms | PASS | - |
| amb-004 | ambiguity | - | 3 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 267.4 ms | PASS | - |
| amb-005 | ambiguity | - | 3 | 200 | TIME_AGGREGATION | True | False | 0 | 0 | 0 | 241.3 ms | PASS | - |
| amb-006 | perception_prefilter | prefilter, terms_aggregation | 3 | 200 | TERMS_AGGREGATION | False | True | 100001 | 100 | 5 | 63.1 ms | PASS | - |
| amb-007 | perception_prefilter | prefilter, terms_aggregation, top_n_required | 3 | 200 | TERMS_AGGREGATION | True | False | 0 | 0 | 0 | 7.0 ms | PASS | - |
| amb-008 | ambiguity | missing_group_by | 3 | 200 | TERMS_AGGREGATION | True | False | 0 | 0 | 0 | 278.9 ms | PASS | - |
| amb-009 | ambiguity | follow_up | 3 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 257.6 ms | PASS | - |
| llm-001 | llm_language | vietnamese, auth, temporal | 3 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 274.6 ms | PASS | - |
| llm-002 | llm_language | mixed_language, statistics | 3 | 200 | TERMS_AGGREGATION | False | True | 100001 | 100 | 1 | 32.3 ms | PASS | - |
| llm-003 | llm_language | time_bucket, trend | 3 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 233.8 ms | PASS | - |
| llm-004 | llm_language | command_noise, free_text | 3 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 243.8 ms | PASS | - |
| llm-005 | llm_language | mitre_like, ambiguous_attack | 3 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 277.6 ms | PASS | - |
| llm-006 | llm_language | explicit_filter_priority | 3 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 239.7 ms | PASS | - |
| tmp-001 | temporal | - | 3 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 260.8 ms | PASS | - |
| tmp-002 | temporal | - | 3 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 267.1 ms | PASS | - |
| tmp-003 | temporal | - | 3 | 200 | TIME_AGGREGATION | True | False | 0 | 0 | 0 | 244.6 ms | PASS | - |
| tmp-004 | temporal | explicit_time, open_range | 3 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 258.3 ms | PASS | - |
| tmp-005 | temporal | explicit_time, pagination | 3 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 266.6 ms | PASS | - |
| tmp-006 | temporal | time_aggregation, explicit_time | 3 | 200 | TIME_AGGREGATION | True | False | 0 | 0 | 0 | 265.6 ms | PASS | - |
| tmp-007 | temporal | quarter, time_aggregation, regression | 3 | 200 | TIME_AGGREGATION | True | False | 0 | 0 | 0 | 253.5 ms | PASS | - |
| res-001 | residual | - | 3 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 253.0 ms | PASS | - |
| res-002 | residual | - | 3 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 244.0 ms | PASS | - |
| res-003 | residual | free_text, message_only | 3 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 250.7 ms | PASS | - |
| res-004 | residual | explicit_filter, free_text | 3 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 260.1 ms | PASS | - |
| res-005 | residual | noise_words | 3 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 261.4 ms | PASS | - |
- `evaluation\cases\workflow\soc_nl2plan_v1.jsonl`
- `evaluation\cases\workflow\ambiguity_cases.jsonl`
- `evaluation\cases\workflow\llm_language_cases.jsonl`
- `evaluation\cases\workflow\temporal_cases.jsonl`
- `evaluation\cases\regression\residual_cases.jsonl`
