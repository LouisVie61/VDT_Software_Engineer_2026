# Backend Evaluation Benchmark

Benchmark nay dung de do backend theo contract hien tai: LLM chiu trach nhiem xu ly ngon ngu/ambiguity, Perception Layer chi la soft prior va optimization, con application layer validate/build canonical plan/DSL.

## Muc tieu

- **Correctness**: HTTP response thanh cong, template/DSL/filter/pagination khop expectation, va query da execute co evidence khi case yeu cau.
- **LLM language quality**: prompt phai xu ly Vietnamese/English/mixed-language, command-word noise, temporal expression, ambiguous SOC wording.
- **Perception optimization**: cac case ro rang co the di fast prefilter; case ambiguous phai quay ve LLM/confirmation thay vi phu thuoc router dung.
- **Stability**: chay lap lai nhieu vong, co warmup, latency p95 theo category, status error rate va zero-result rate.
- **Auditability**: report markdown va optional JSON co case-level checks, failed checks, template distribution, confirmation/cache rate.

## Cau truc

```text
evaluation/
+-- README.md
+-- annotation_guideline.md
+-- cases/
|   +-- soc_nl2plan_v1.jsonl
|   +-- temporal_cases.jsonl
|   +-- residual_cases.jsonl
|   +-- ambiguity_cases.jsonl
|   +-- llm_language_cases.jsonl
+-- reports/
|   +-- .gitkeep
+-- evaluator.py
+-- metrics.py
+-- runner.py
```

## Chay benchmark

1. Start backend:

```powershell
cd demo
.\mvnw spring-boot:run
```

2. Chay evaluation trong terminal khac:

```powershell
cd demo
python evaluation\runner.py --base-url http://localhost:8080 --warmup 1 --repeat 3
```

Mac/Linux:

```bash
cd demo
python3 evaluation/runner.py --base-url http://localhost:8080 --warmup 1 --repeat 3
```

Report mac dinh duoc ghi vao:

```text
demo/evaluation/reports/backend_evaluation_report.md
```

## Tuy chon

```powershell
python evaluation\runner.py `
  --base-url http://localhost:8080 `
  --cases evaluation\cases\soc_nl2plan_v1.jsonl evaluation\cases\temporal_cases.jsonl `
  --repeat 5 `
  --warmup 1 `
  --timeout 10 `
  --output evaluation\reports\custom_report.md `
  --json-output evaluation\reports\custom_report.json
```

## Benchmark profiles

Default suite chay tat ca case, bao gom `llm_language_cases.jsonl`. Neu muon benchmark backend contract ma khong phu thuoc LLM provider/API key, chay subset on dinh:

```powershell
python evaluation\runner.py `
  --cases evaluation\cases\soc_nl2plan_v1.jsonl evaluation\cases\temporal_cases.jsonl evaluation\cases\residual_cases.jsonl evaluation\cases\ambiguity_cases.jsonl `
  --repeat 3
```

Neu muon benchmark prompt/LLM language quality rieng:

```powershell
python evaluation\runner.py `
  --cases evaluation\cases\llm_language_cases.jsonl `
  --repeat 3 `
  --timeout 15
```

## Tieu chuan pass mac dinh

- HTTP status phai la `200`, tru khi case khai bao status khac.
- Moi expectation trong case phai pass.
- Neu response khong phai confirmation, `totalCount` phai lon hon `0` va `results` hoac `aggregations` phai khac rong, tru khi case khai bao `expected.allowZeroResults=true`.
- Response confirmation la first-class contract: case confidence thap nen assert `needsConfirmation=true` va `confirmation.confirmationId`, khong assert DSL execution.
- Aggregation execution tra ve bucket aggregation kem hits page de phuc vu results/export, nen benchmark aggregation ro rang co the ky vong `generatedDsl.size`, `sort`, `results`, va `aggregations` cung ton tai.
- Latency moi request phai nho hon `thresholds.max_latency_ms` neu case co khai bao.
- Benchmark tong hop pass khi case pass rate dat toi thieu `90%`.

## Expectation helpers

- `equals`, `notEquals`, `fieldIn`, `fieldExists`, `fieldNotExists`: kiem tra path tren response.
- `generatedDslContains`, `generatedDslNotContains`, `generatedDslAnyContains`: kiem tra compact JSON DSL.
- `generatedDslPath.equals`, `generatedDslPath.exists`, `generatedDslPath.notExists`, `generatedDslPath.fieldIn`: kiem tra path truc tiep tren `generatedDsl`.
- `anyOf`: cho phep nhieu response hop le, vi ambiguous query co the confirmation hoac execute neu du confidence.
- `tags`: gan nhan benchmark case de doc report nhanh hon.

Day la benchmark nhe, phu hop CI/manual audit. Neu can benchmark tai nang, nen tach thanh load test rieng vi muc tieu o day la correctness/stability cua backend contract.
