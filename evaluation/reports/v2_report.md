# Backend Evaluation Report

- Generated at: `2026-07-03 04:37:07 UTC`
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
| Confirmation runs | 18 |
| Confirmations followed | 18 |
| Confirmation rate | 64.3% |
| Cache hits | 0 |
| Cache hit rate | 0.0% |
| Zero-result runs | 14 |
| Zero-result rate | 50.0% |
| Latency min | 79.1 ms |
| Latency median | 360.6 ms |
| Latency p95 | 4327.2 ms |
| Latency max | 5089.5 ms |

## Category Breakdown

| Category | Runs | Pass Rate | Latency p95 |
|---|---:|---:|---:|
| ambiguity | 7 | 100.0% | 4207.6 ms |
| api_contract | 4 | 100.0% | 3746.6 ms |
| llm_language | 6 | 83.3% | 960.9 ms |
| perception_prefilter | 2 | 100.0% | 311.6 ms |
| soc_nl2plan | 2 | 100.0% | 4994.6 ms |
| temporal | 7 | 85.7% | 350.2 ms |

## Metric Group Scorecard

| Metric group | Runs | Run pass rate | Checks | Check pass rate | Latency p95 |
|---|---:|---:|---:|---:|---:|
| DSL Correctness | 17 | 100.0% | 63 | 100.0% | 4694.0 ms |
| Aggregation Correctness | 11 | 81.8% | 59 | 96.6% | 1959.9 ms |
| Result Quality | 28 | 100.0% | 225 | 100.0% | 4327.2 ms |
| Safety / Guardrail | 19 | 100.0% | 70 | 100.0% | 3432.7 ms |

## Template Distribution

| Template | Runs |
|---|---:|
| SIMPLE_SEARCH | 17 |
| TERMS_AGGREGATION | 6 |
| TIME_AGGREGATION | 5 |

## Case Results

| Case | Category | Tags | Iteration | Status | Template | Confirm | Cache | Total | Rows | Aggs | Latency | Result | Failed Checks |
|---|---|---|---:|---:|---|---:|---:|---:|---:|---:|---:|---|---|
| soc-001 | soc_nl2plan | - | 1 | 200 | SIMPLE_SEARCH | False | False | 3142 | 50 | 0 | 5089.5 ms | PASS | - |
| soc-002 | soc_nl2plan | - | 1 | 200 | SIMPLE_SEARCH | False | False | 0 | 0 | 0 | 3190.3 ms | PASS | - |
| soc-003 | api_contract | - | 1 | 200 | SIMPLE_SEARCH | False | False | 100001 | 50 | 0 | 2753.9 ms | PASS | - |
| soc-004 | api_contract | - | 1 | 200 | SIMPLE_SEARCH | False | False | 0 | 0 | 0 | 3829.6 ms | PASS | - |
| soc-005 | api_contract | - | 1 | 200 | SIMPLE_SEARCH | False | False | 1 | 0 | 0 | 3276.2 ms | PASS | - |
| soc-006 | api_contract | - | 1 | 200 | SIMPLE_SEARCH | False | False | 0 | 0 | 0 | 3116.7 ms | PASS | - |
| amb-001 | ambiguity | - | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 1080.5 ms | PASS | - |
| amb-002 | ambiguity | - | 1 | 200 | TERMS_AGGREGATION | False | False | 100001 | 500 | 1 | 153.6 ms | PASS | - |
| amb-003 | ambiguity | - | 1 | 200 | TERMS_AGGREGATION | True | False | 100001 | 50 | 8 | 985.1 ms | PASS | - |
| amb-004 | ambiguity | - | 1 | 200 | SIMPLE_SEARCH | False | False | 1 | 1 | 0 | 4595.1 ms | PASS | - |
| amb-005 | ambiguity | - | 1 | 200 | TIME_AGGREGATION | True | False | 100001 | 50 | 41 | 2934.8 ms | PASS | - |
| amb-006 | perception_prefilter | - | 1 | 200 | TERMS_AGGREGATION | False | False | 100001 | 100 | 5 | 320.1 ms | PASS | - |
| amb-007 | perception_prefilter | - | 1 | 200 | TERMS_AGGREGATION | True | False | 100001 | 100 | 10 | 149.9 ms | PASS | - |
| amb-008 | ambiguity | - | 1 | 200 | TERMS_AGGREGATION | True | False | 100001 | 50 | 8 | 329.8 ms | PASS | - |
| amb-009 | ambiguity | - | 1 | 200 | SIMPLE_SEARCH | True | False | 17711 | 50 | 0 | 3303.6 ms | PASS | - |
| llm-001 | llm_language | - | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 1158.8 ms | PASS | - |
| llm-002 | llm_language | - | 1 | 200 | TERMS_AGGREGATION | False | False | 100001 | 100 | 1 | 79.1 ms | FAIL | `final:dsl_any_contains:['error', 'failure']` no alternative fragment found |
| llm-003 | llm_language | - | 1 | 200 | TIME_AGGREGATION | True | False | 0 | 0 | 0 | 364.9 ms | PASS | - |
| llm-004 | llm_language | - | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 367.3 ms | PASS | - |
| llm-005 | llm_language | - | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 329.7 ms | PASS | - |
| llm-006 | llm_language | - | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 342.8 ms | PASS | - |
| tmp-001 | temporal | - | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 335.8 ms | PASS | - |
| tmp-002 | temporal | - | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 294.6 ms | PASS | - |
| tmp-003 | temporal | - | 1 | 200 | TIME_AGGREGATION | True | False | 980 | 100 | 359 | 356.3 ms | PASS | - |
| tmp-004 | temporal | - | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 314.4 ms | PASS | - |
| tmp-005 | temporal | - | 1 | 200 | SIMPLE_SEARCH | True | False | 0 | 0 | 0 | 312.1 ms | PASS | - |
| tmp-006 | temporal | - | 1 | 200 | TIME_AGGREGATION | True | False | 7878 | 100 | 360 | 328.0 ms | FAIL | `final:dsl_any_contains:['"fixed_interval":"1d"', '"calendar_interval":"1d"', '"calendar_interval":"day"']` no alternative fragment found |
| tmp-007 | temporal | - | 1 | 200 | TIME_AGGREGATION | True | False | 628 | 50 | 4 | 307.1 ms | PASS | - |
- `evaluation\cases\v2_cases.jsonl`
