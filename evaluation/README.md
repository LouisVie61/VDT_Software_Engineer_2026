# V1 LLM-only benchmark

Thư mục này dùng để đánh giá V1: LLM trực tiếp diễn giải câu hỏi và sinh truy vấn, không chạy controlled confirmation workflow của V2.

## Benchmark workload

- `evaluation/cases/llm_cases.jsonl`: workload V1 duy nhất, gồm 28 case.
- Elasticsearch index `soc-events`: execution corpus; phải giữ cùng snapshot trong toàn bộ lần đo.
- `evaluation/reports/`: chỉ chứa `.gitkeep`; report chạy benchmark không commit vào source.

## Chạy benchmark

Chạy backend trong cửa sổ PowerShell thứ nhất:

```powershell
cd D:\VDT_SE_WEB_2026_Main\demo
$env:APP_LLM_PROVIDER_ORDER = "GEMINI"
$env:GEMINI_MODEL = "gemini-2.5-flash"
if ([string]::IsNullOrWhiteSpace($env:GEMINI_API_KEY)) { throw "Set GEMINI_API_KEY first." }
mvn spring-boot:run
```

Chạy benchmark trong cửa sổ thứ hai:

```powershell
cd D:\VDT_SE_WEB_2026_Main\demo
python evaluation\runner.py `
  --variant llm-only-v1 `
  --base-url http://localhost:8080 `
  --cases evaluation\cases\llm_cases.jsonl `
  --require-executions 28 `
  --repeat 1 `
  --warmup 0 `
  --timeout 300 `
  --data-snapshot <soc-events-snapshot-id> `
  --provider-config gemini-2.5-flash `
  --cache-regime cold `
  --output evaluation\reports\v1_report.md `
  --json-output evaluation\reports\v1_report.json
```

Không dùng `--auto-confirm` trên nhánh V1. Snapshot, model/config và cache regime phải được ghi đúng theo môi trường chạy thực tế.

## Cấu trúc giữ lại

```text
evaluation/
├── cases/llm_cases.jsonl
├── reports/.gitkeep
├── evaluator.py
├── metrics.py
├── runner.py
└── README.md
```
