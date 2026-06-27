# Backend Evaluation Report

- Generated at: `2026-06-27 04:08:19 UTC`
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
| Passed | 15 |
| Failed | 13 |
| Pass rate | 53.6% |
| Status error rate | 0.0% |
| Executed runs | 28 |
| Confirmation runs | 0 |
| Confirmation rate | 0.0% |
| Cache hits | 0 |
| Cache hit rate | 0.0% |
| Zero-result runs | 3 |
| Zero-result rate | 10.7% |
| Latency min | 38006.7 ms |
| Latency median | 58506.3 ms |
| Latency p95 | 87558.8 ms |
| Latency max | 96750.9 ms |

## Category Breakdown

| Category | Runs | Pass Rate | Latency p95 |
|---|---:|---:|---:|
| baseline | 28 | 53.6% | 87558.8 ms |

## Metric Group Scorecard

| Metric group | Runs | Pass rate | Latency p95 |
|---|---:|---:|---:|
| Aggregation Correctness | 8 | 0.0% | 85919.2 ms |
| DSL Correctness | 28 | 53.6% | 87558.8 ms |
| Performance | 28 | 100.0% | 87558.8 ms |
| Result Quality | 8 | 37.5% | 85147.6 ms |
| Safety / Guardrail | 4 | 100.0% | 92780.9 ms |

## Template Distribution

| Template | Runs |
|---|---:|
| none | 28 |

## Case Results

| Case | Category | Tags | Iteration | Status | Template | Confirm | Cache | Total | Rows | Aggs | Latency | Result | Failed Checks |
|---|---|---|---:|---:|---|---:|---:|---:|---:|---:|---:|---|---|
| soc-001 | baseline | - | 1 | 200 | - | None | None | 3142 | 50 | 0 | 76664.8 ms | PASS | - |
| soc-002 | baseline | - | 1 | 200 | - | None | None | 10000 | 50 | 0 | 56413.9 ms | PASS | - |
| soc-003 | baseline | - | 1 | 200 | - | None | None | 10000 | 50 | 0 | 59292.1 ms | PASS | - |
| soc-004 | baseline | - | 1 | 200 | - | None | None | 10000 | 50 | 0 | 70949.6 ms | PASS | - |
| soc-005 | baseline | - | 1 | 200 | - | None | None | 2 | 2 | 0 | 65960.3 ms | PASS | - |
| soc-006 | baseline | - | 1 | 200 | - | None | None | 10000 | 50 | 0 | 56352.1 ms | PASS | - |
| amb-001 | baseline | - | 1 | 200 | - | None | None | 28 | 28 | 0 | 70284.5 ms | PASS | - |
| amb-002 | baseline | - | 1 | 200 | - | None | None | 10000 | 50 | 0 | 61293.8 ms | FAIL | `non_empty:aggregations` actual=[]<br>`dsl_contains:"aggs"` fragment not found<br>`dsl_contains:"terms"` fragment not found |
| amb-003 | baseline | - | 1 | 200 | - | None | None | 0 | 0 | 0 | 83553.7 ms | PASS | - |
| amb-004 | baseline | - | 1 | 200 | - | None | None | 18 | 18 | 0 | 48526.7 ms | PASS | - |
| amb-005 | baseline | - | 1 | 200 | - | None | None | 0 | 0 | 0 | 74288.7 ms | PASS | - |
| amb-006 | baseline | - | 1 | 200 | - | None | None | 54 | 50 | 0 | 57720.5 ms | FAIL | `non_empty:aggregations` actual=[]<br>`dsl_contains:"aggs"` fragment not found<br>`dsl_contains:"terms"` fragment not found |
| amb-007 | baseline | - | 1 | 200 | - | None | None | 10000 | 50 | 0 | 62171.1 ms | FAIL | `dsl_contains:"aggs"` fragment not found |
| amb-008 | baseline | - | 1 | 200 | - | None | None | 27 | 27 | 0 | 53741.9 ms | PASS | - |
| amb-009 | baseline | - | 1 | 200 | - | None | None | 10000 | 50 | 0 | 52350.2 ms | PASS | - |
| llm-001 | baseline | - | 1 | 200 | - | None | None | 24 | 24 | 0 | 38006.7 ms | FAIL | `dsl_any_contains:['auth', 'login']` no alternative fragment found<br>`dsl_any_contains:['failed', 'failure']` no alternative fragment found |
| llm-002 | baseline | - | 1 | 200 | - | None | None | 55 | 50 | 0 | 89715.3 ms | FAIL | `non_empty:aggregations` actual=[]<br>`dsl_contains:"aggs"` fragment not found<br>`dsl_contains:"terms"` fragment not found |
| llm-003 | baseline | - | 1 | 200 | - | None | None | 10000 | 50 | 0 | 78869.3 ms | FAIL | `dsl_contains:"aggs"` fragment not found<br>`dsl_contains:date_histogram` fragment not found |
| llm-004 | baseline | - | 1 | 200 | - | None | None | 3577 | 50 | 0 | 56116.6 ms | PASS | - |
| llm-005 | baseline | - | 1 | 200 | - | None | None | 23 | 23 | 0 | 96750.9 ms | PASS | - |
| llm-006 | baseline | - | 1 | 200 | - | None | None | 3116 | 50 | 0 | 65182.3 ms | PASS | - |
| tmp-001 | baseline | - | 1 | 200 | - | None | None | 10000 | 50 | 0 | 52257.7 ms | FAIL | `dsl_contains:"range"` fragment not found<br>`dsl_contains:2025-06-01` fragment not found |
| tmp-002 | baseline | - | 1 | 200 | - | None | None | 10000 | 50 | 0 | 63558.3 ms | FAIL | `dsl_contains:"range"` fragment not found<br>`dsl_contains:2025-06` fragment not found |
| tmp-003 | baseline | - | 1 | 200 | - | None | None | 10000 | 50 | 0 | 57152.3 ms | FAIL | `non_empty:aggregations` actual=[]<br>`dsl_contains:date_histogram` fragment not found<br>`dsl_contains:2025-06-01` fragment not found |
| tmp-004 | baseline | - | 1 | 200 | - | None | None | 10000 | 50 | 0 | 38863.0 ms | FAIL | `dsl_contains:"range"` fragment not found<br>`dsl_contains:2025-06-01` fragment not found |
| tmp-005 | baseline | - | 1 | 200 | - | None | None | 10000 | 50 | 0 | 45024.0 ms | FAIL | `dsl_contains:"range"` fragment not found<br>`dsl_contains:2025-06` fragment not found |
| tmp-006 | baseline | - | 1 | 200 | - | None | None | 10000 | 50 | 0 | 55137.2 ms | FAIL | `non_empty:aggregations` actual=[]<br>`dsl_contains:date_histogram` fragment not found<br>`dsl_contains:2025-06-01` fragment not found |
| tmp-007 | baseline | - | 1 | 200 | - | None | None | 0 | 0 | 0 | 52705.1 ms | FAIL | `dsl_contains:"aggs"` fragment not found<br>`dsl_contains:date_histogram` fragment not found |
- `evaluation\cases\ablation\workflow_comparison_cases.jsonl`
