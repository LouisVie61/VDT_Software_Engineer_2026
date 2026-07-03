# Backend Evaluation Report

- Generated at: `2026-07-01 12:40:04 UTC`
- Backend: `http://localhost:8080`
- Python: `3.12.3`
- Cases: `28`
- Repeat: `1`
- Warmup: `0`
- Variant: `workflow-v2`
- Data snapshot: `soc-events-fixed`
- Provider config: `current-model-config`
- Cache regime: `cold`
- Request fingerprint: `4b78e1053d2e30d997558e7079cd19e5b6297824320b202a8de5d272657dda8d`
- Evaluation fingerprint: `84a3146ecb2accbd60ad2aa669e6ea2ed310863bd5061e18f2de8bffc8b143c3`
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
| Confirmation runs | 20 |
| Confirmations followed | 20 |
| Confirmation rate | 71.4% |
| Cache hits | 0 |
| Cache hit rate | 0.0% |
| Zero-result runs | 15 |
| Zero-result rate | 53.6% |
| Latency min | 69.2 ms |
| Latency median | 366.9 ms |
| Latency p95 | 4577.3 ms |
| Latency max | 6610.9 ms |

## Category Breakdown

| Category | Runs | Pass Rate | Latency p95 |
|---|---:|---:|---:|
| ambiguity | 7 | 100.0% | 2487.0 ms |
| api_contract | 4 | 100.0% | 6131.5 ms |
| llm_language | 6 | 83.3% | 396.7 ms |
| perception_prefilter | 2 | 100.0% | 217.2 ms |
| soc_nl2plan | 2 | 100.0% | 5091.8 ms |
| temporal | 7 | 85.7% | 373.0 ms |

## Metric Group Scorecard

| Metric group | Runs | Run pass rate | Checks | Check pass rate | Latency p95 |
|---|---:|---:|---:|---:|---:|
| DSL Correctness | 23 | 95.7% | 94 | 98.9% | 5024.5 ms |
| Aggregation Correctness | 5 | 80.0% | 28 | 96.4% | 1540.5 ms |
| Result Quality | 28 | 100.0% | 227 | 100.0% | 4577.3 ms |
| Safety / Guardrail | 20 | 100.0% | 76 | 100.0% | 2787.0 ms |

## Template Distribution

| Template | Runs |
|---|---:|
| SIMPLE_SEARCH | 17 |
| TERMS_AGGREGATION | 6 |
| TIME_AGGREGATION | 5 |

## Case Results

| Case | Category | Tags | Iteration | Status | Template | Confirm | Cache | Total | Rows | Aggs | Latency | Result | Failed Checks |
|---|---|---|---:|---:|---|---:|---:|---:|---:|---:|---:|---|---|
| soc-001 | soc_nl2plan | - | 1 | 200 | SIMPLE_SEARCH | False | False | 3142 | 50 | 0 | 5203.3 ms | PASS | - |
| soc-002 | soc_nl2plan | - | 1 | 200 | SIMPLE_SEARCH | False | False | 0 | 0 | 0 | 2972.3 ms | PASS | - |
| soc-003 | api_contract | - | 1 | 200 | SIMPLE_SEARCH | True | False | 100001 | 50 | 0 | 3153.8 ms | PASS | - |
| soc-004 | api_contract | - | 1 | 200 | SIMPLE_SEARCH | False | False | 0 | 0 | 0 | 6610.9 ms | PASS | - |
| soc-005 | api_contract | - | 1 | 200 | SIMPLE_SEARCH | False | False | 1 | 0 | 0 | 3233.1 ms | PASS | - |
| soc-006 | api_contract | - | 1 | 200 | SIMPLE_SEARCH | False | False | 0 | 0 | 0 | 3414.6 ms | PASS | - |
| amb-001 | ambiguity | - | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 2767.7 ms | PASS | - |
| amb-002 | ambiguity | - | 1 | 200 | TERMS_AGGREGATION | False | False | 100001 | 500 | 1 | 275.5 ms | PASS | - |
| amb-003 | ambiguity | - | 1 | 200 | TERMS_AGGREGATION | True | False | 100001 | 50 | 8 | 1610.8 ms | PASS | - |
| amb-004 | ambiguity | - | 1 | 200 | SIMPLE_SEARCH | True | False | 1 | 1 | 0 | 1286.3 ms | PASS | - |
| amb-005 | ambiguity | - | 1 | 200 | TIME_AGGREGATION | True | False | 100001 | 50 | 41 | 1832.1 ms | PASS | - |
| amb-006 | perception_prefilter | - | 1 | 200 | TERMS_AGGREGATION | False | False | 100001 | 100 | 5 | 221.9 ms | PASS | - |
| amb-007 | perception_prefilter | - | 1 | 200 | TERMS_AGGREGATION | True | False | 100001 | 100 | 10 | 128.2 ms | PASS | - |
| amb-008 | ambiguity | - | 1 | 200 | TERMS_AGGREGATION | True | False | 100001 | 50 | 8 | 357.5 ms | PASS | - |
| amb-009 | ambiguity | - | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 384.0 ms | PASS | - |
| llm-001 | llm_language | - | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 412.3 ms | PASS | - |
| llm-002 | llm_language | - | 1 | 200 | TERMS_AGGREGATION | False | False | 100001 | 100 | 1 | 69.2 ms | FAIL | `final:dsl_any_contains:['error', 'failure']` no alternative fragment found |
| llm-003 | llm_language | - | 1 | 200 | TIME_AGGREGATION | True | False | 0 | 0 | 0 | 337.3 ms | PASS | - |
| llm-004 | llm_language | - | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 306.1 ms | PASS | - |
| llm-005 | llm_language | - | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 350.2 ms | PASS | - |
| llm-006 | llm_language | - | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 343.5 ms | PASS | - |
| tmp-001 | temporal | - | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 350.2 ms | PASS | - |
| tmp-002 | temporal | - | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 345.5 ms | PASS | - |
| tmp-003 | temporal | - | 1 | 200 | TIME_AGGREGATION | True | False | 980 | 100 | 359 | 363.3 ms | PASS | - |
| tmp-004 | temporal | - | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 348.6 ms | PASS | - |
| tmp-005 | temporal | - | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 327.5 ms | PASS | - |
| tmp-006 | temporal | - | 1 | 200 | TIME_AGGREGATION | True | False | 7878 | 100 | 360 | 374.1 ms | FAIL | `final:dsl_any_contains:['"fixed_interval":"1d"', '"calendar_interval":"1d"', '"calendar_interval":"day"']` no alternative fragment found |
| tmp-007 | temporal | - | 1 | 200 | TIME_AGGREGATION | True | False | 628 | 50 | 4 | 370.5 ms | PASS | - |
- `evaluation\cases\v2_cases.jsonl`
