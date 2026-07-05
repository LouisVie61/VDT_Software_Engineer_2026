# Backend Evaluation Report

- Generated at: `2026-07-04 20:45:25 UTC`
- Backend: `http://localhost:8080`
- Python: `3.12.3`
- Cases: `28`
- Repeat: `1`
- Warmup: `0`
- Variant: `workflow-v3`
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
| Cache hits | 0 |
| Cache hit rate | 0.0% |
| Zero-result runs | 13 |
| Zero-result rate | 46.4% |
| Latency min | 867.9 ms |
| Latency median | 1002.1 ms |
| Latency p95 | 1274.3 ms |
| Latency max | 13371.0 ms |

## Category Breakdown

| Category | Runs | Pass Rate | Latency p95 |
|---|---:|---:|---:|
| ambiguity | 7 | 85.7% | 9696.5 ms |
| api_contract | 4 | 75.0% | 1148.9 ms |
| llm_language | 6 | 100.0% | 1024.7 ms |
| perception_prefilter | 2 | 100.0% | 941.8 ms |
| soc_nl2plan | 2 | 100.0% | 1313.9 ms |
| temporal | 7 | 100.0% | 1111.2 ms |

## Metric Group Scorecard

| Metric group | Runs | Run pass rate | Checks | Check pass rate | Latency p95 |
|---|---:|---:|---:|---:|---:|
| DSL Correctness | 17 | 88.2% | 63 | 96.8% | 3737.7 ms |
| Aggregation Correctness | 11 | 100.0% | 59 | 100.0% | 1064.1 ms |
| Result Quality | 28 | 100.0% | 123 | 100.0% | 1274.3 ms |
| Safety / Guardrail | 6 | 100.0% | 16 | 100.0% | 10308.9 ms |

## Template Distribution

| Template | Runs |
|---|---:|
| IQL | 28 |

## Case Results

| Case | Category | Tags | Iteration | Status | Template | Cache | Total | Rows | Aggs | Latency | Result | Failed Checks |
|---|---|---|---:|---:|---|---:|---:|---:|---:|---:|---|---|
| soc-001 | soc_nl2plan | v3, direct_iql | 1 | 200 | IQL | False | 0 | 0 | 0 | 1329.4 ms | PASS | - |
| soc-002 | soc_nl2plan | v3, direct_iql | 1 | 200 | IQL | False | 0 | 0 | 0 | 1019.6 ms | PASS | - |
| soc-003 | api_contract | v3, direct_iql | 1 | 200 | IQL | False | 109949 | 50 | 0 | 895.3 ms | PASS | - |
| soc-004 | api_contract | v3, direct_iql | 1 | 200 | IQL | False | 15490 | 30 | 0 | 1171.8 ms | FAIL | `dsl_contains:endpoint` fragment not found |
| soc-005 | api_contract | v3, direct_iql | 1 | 200 | IQL | False | 6597 | 10 | 0 | 1019.0 ms | PASS | - |
| soc-006 | api_contract | v3, direct_iql | 1 | 200 | IQL | False | 0 | 0 | 0 | 1003.6 ms | PASS | - |
| amb-001 | ambiguity | v3, direct_iql | 1 | 200 | IQL | False | 109949 | 50 | 0 | 867.9 ms | PASS | - |
| amb-002 | ambiguity | v3, direct_iql | 1 | 200 | IQL | False | 109949 | 0 | 10 | 1000.5 ms | PASS | - |
| amb-003 | ambiguity | v3, direct_iql | 1 | 200 | IQL | False | 109949 | 0 | 9 | 1016.1 ms | PASS | - |
| amb-004 | ambiguity | v3, direct_iql | 1 | 200 | IQL | False | 0 | 0 | 0 | 1122.8 ms | PASS | - |
| amb-005 | ambiguity | v3, direct_iql | 1 | 200 | IQL | False | 0 | 0 | 0 | 897.5 ms | PASS | - |
| amb-006 | perception_prefilter | v3, direct_iql | 1 | 200 | IQL | False | 109949 | 0 | 5 | 938.1 ms | PASS | - |
| amb-007 | perception_prefilter | v3, direct_iql | 1 | 200 | IQL | False | 109949 | 0 | 6 | 942.0 ms | PASS | - |
| amb-008 | ambiguity | v3, direct_iql | 1 | 200 | IQL | False | 0 | 0 | 0 | 1034.7 ms | PASS | - |
| amb-009 | ambiguity | v3, direct_iql | 1 | 200 | IQL | False | 0 | 0 | 0 | 13371.0 ms | FAIL | `dsl_contains:"severity":"critical"` fragment not found |
| llm-001 | llm_language | v3, direct_iql | 1 | 200 | IQL | False | 0 | 0 | 0 | 1018.3 ms | PASS | - |
| llm-002 | llm_language | v3, direct_iql | 1 | 200 | IQL | False | 0 | 0 | 0 | 1026.8 ms | PASS | - |
| llm-003 | llm_language | v3, direct_iql | 1 | 200 | IQL | False | 109949 | 0 | 1 | 923.5 ms | PASS | - |
| llm-004 | llm_language | v3, direct_iql | 1 | 200 | IQL | False | 0 | 0 | 0 | 952.9 ms | PASS | - |
| llm-005 | llm_language | v3, direct_iql | 1 | 200 | IQL | False | 0 | 0 | 0 | 875.2 ms | PASS | - |
| llm-006 | llm_language | v3, direct_iql | 1 | 200 | IQL | False | 0 | 0 | 0 | 930.3 ms | PASS | - |
| tmp-001 | temporal | v3, direct_iql | 1 | 200 | IQL | False | 7878 | 50 | 0 | 971.1 ms | PASS | - |
| tmp-002 | temporal | v3, direct_iql | 1 | 200 | IQL | False | 744 | 50 | 0 | 1118.7 ms | PASS | - |
| tmp-003 | temporal | v3, direct_iql | 1 | 200 | IQL | False | 980 | 0 | 15 | 1093.6 ms | PASS | - |
| tmp-004 | temporal | v3, direct_iql | 1 | 200 | IQL | False | 28518 | 50 | 0 | 1041.8 ms | PASS | - |
| tmp-005 | temporal | v3, direct_iql | 1 | 200 | IQL | False | 1937 | 15 | 0 | 931.4 ms | PASS | - |
| tmp-006 | temporal | v3, direct_iql | 1 | 200 | IQL | False | 7878 | 0 | 15 | 952.2 ms | PASS | - |
| tmp-007 | temporal | v3, direct_iql | 1 | 200 | IQL | False | 0 | 0 | 0 | 934.3 ms | PASS | - |
- `evaluation\cases\v3_cases.jsonl`
