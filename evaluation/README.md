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
|   +-- workflow/
|   |   +-- soc_nl2plan_v1.jsonl
|   |   +-- ambiguity_cases.jsonl
|   |   +-- llm_language_cases.jsonl
|   |   +-- temporal_cases.jsonl
|   +-- regression/
|   |   +-- residual_cases.jsonl
|   +-- ablation/
|   |   +-- ab_execution_cases.jsonl
|   |   +-- workflow_comparison_cases.jsonl
|   |   +-- llm_cases.jsonl
+-- reports/
|   +-- .gitkeep
+-- evaluator.py
+-- compare_ab.py
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
  --cases evaluation\cases\workflow\soc_nl2plan_v1.jsonl evaluation\cases\workflow\temporal_cases.jsonl `
  --repeat 5 `
  --warmup 1 `
  --timeout 10 `
  --output evaluation\reports\custom_report.md `
  --json-output evaluation\reports\custom_report.json
```

## Benchmark profiles

### Ablation metric scorecard

Suite `workflow_comparison_cases.jsonl` duoc cham black-box theo 5 nhom:

- `DSL Correctness`: template, generated DSL, filter, pagination va time range.
- `Aggregation Correctness`: terms/time aggregation, bucket va chart contract.
- `Result Quality`: HTTP/JSON contract, execution evidence, result va total count.
- `Safety / Guardrail`: confirmation, warning, ambiguity va prohibited DSL behavior.
- `Performance`: latency budget cua tung case va latency p95.

Moi assertion trong JSON report co field `metric_group`. Scorecard Markdown va
JSON cung bao cao ca run pass rate va check pass rate, nen loi cua mot nhom
khong lam sai diem cua nhom khac.

```powershell
python evaluation\runner.py `
  --base-url http://localhost:8080 `
  --cases evaluation\cases\ablation\workflow_comparison_cases.jsonl `
  --repeat 1 `
  --timeout 30 `
  --output evaluation\reports\ablation_report.md `
  --json-output evaluation\reports\ablation_report.json
```

### LLM baseline

Suite `llm_cases.jsonl` gom 28 cau hoi baseline, duoc tach khoi workflow contract
de danh gia luong LLM truc tiep theo DSL correctness, aggregation evidence,
result quality, safety va latency thuc do.

```powershell
python evaluation\runner.py `
  --base-url http://localhost:8080 `
  --cases evaluation\cases\ablation\llm_cases.jsonl `
  --repeat 1 `
  --warmup 0 `
  --output evaluation\reports\llm_baseline_benchmark.md
```

### Fair version comparison protocol

Dung `ab_execution_cases.jsonl` cho ca hai nhanh. File nay giu cung case ID,
request payload va final expectation. Khong dung `llm_cases.jsonl` cho baseline
roi `workflow_comparison_cases.jsonl` cho V2 trong mot controlled A/B, vi payload
va semantics cua `pass` khac nhau.

V2 chay voi `--auto-confirm`: response `needsConfirmation=true` duoc follow qua
`POST /api/search/confirm`, sau do evaluator cham toan bo final expectation tren
response cuoi. Initial confirmation van duoc cham rieng ve confirmation ID/intent.
`--require-executions 28` lam benchmark fail neu khong du 28 final execution.

Truoc moi nhanh, khoi phuc cung Elasticsearch snapshot va dua Redis ve cung cache
state. Hai lenh phai khai bao cung `--data-snapshot`, `--provider-config` va
`--cache-regime`; cac label nay la audit metadata, nguoi chay van phai dam bao
external state thuc su trung khop.

Baseline:

```powershell
python evaluation\runner.py `
  --variant llm-baseline `
  --base-url http://localhost:8081 `
  --cases evaluation\cases\ablation\ab_execution_cases.jsonl `
  --require-executions 28 `
  --repeat 1 `
  --warmup 0 `
  --timeout 120 `
  --data-snapshot soc-events-<snapshot-id> `
  --provider-config <provider-model-config-hash> `
  --cache-regime cold `
  --output evaluation\reports\ab_baseline.md `
  --json-output evaluation\reports\ab_baseline.json
```

Workflow V2:

```powershell
python evaluation\runner.py `
  --variant workflow-v2 `
  --base-url http://localhost:8080 `
  --cases evaluation\cases\ablation\ab_execution_cases.jsonl `
  --auto-confirm `
  --require-executions 28 `
  --repeat 1 `
  --warmup 0 `
  --timeout 120 `
  --data-snapshot soc-events-<snapshot-id> `
  --provider-config <provider-model-config-hash> `
  --cache-regime cold `
  --output evaluation\reports\report_v2.md `
  --json-output evaluation\reports\report_v2.json
```

Tao bao cao A/B ghep cap:

```powershell
python evaluation\compare_ab.py `
  --baseline evaluation\reports\ab_baseline.json `
  --candidate evaluation\reports\report_v2.json `
  --output evaluation\reports\ab_comparison.md
```

`compare_ab.py` chi ghi nhan `Controlled paired A/B` khi request/evaluation
fingerprint, case count, repeat, warmup, data snapshot, provider config, cache
regime va full-execution parity deu dat. Bao cao kem transition theo case va
McNemar exact p-value cho outcome pass/fail ghep cap.

Default suite chay cac case `workflow/` va `regression/` de test mot version backend binh thuong. Nhom `ablation/` khong nam trong default suite; dung rieng de black-box compare hai phien ban.

Neu muon benchmark backend contract ma khong phu thuoc LLM provider/API key, chay subset on dinh:

```powershell
python evaluation\runner.py `
  --cases evaluation\cases\workflow\soc_nl2plan_v1.jsonl evaluation\cases\workflow\temporal_cases.jsonl evaluation\cases\regression\residual_cases.jsonl evaluation\cases\workflow\ambiguity_cases.jsonl `
  --repeat 3
```

Neu muon benchmark prompt/LLM language quality rieng:

```powershell
python evaluation\runner.py `
  --cases evaluation\cases\workflow\llm_language_cases.jsonl `
  --repeat 3 `
  --timeout 15
```

Neu muon black-box compare nhanh hai phien ban backend ma khong can ket luan nhan qua, co the chay `workflow_comparison_cases.jsonl` tren tung version. De ket luan A/B, dung protocol `ab_execution_cases.jsonl` o tren.

```powershell
python evaluation\runner.py `
  --base-url http://localhost:8080 `
  --cases evaluation\cases\ablation\workflow_comparison_cases.jsonl `
  --repeat 3 `
  --output evaluation\reports\ablation_v1_report.md `
  --json-output evaluation\reports\ablation_v1_report.json

python evaluation\runner.py `
  --base-url http://localhost:8081 `
  --cases evaluation\cases\ablation\workflow_comparison_cases.jsonl `
  --repeat 3 `
  --output evaluation\reports\ablation_v2_report.md `
  --json-output evaluation\reports\ablation_v2_report.json
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
