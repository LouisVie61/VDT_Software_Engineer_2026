# V2 controlled-workflow benchmark

Benchmark này đánh giá V2 bằng workload Y′ được sinh xác định từ bộ case nguồn X.

- X: `evaluation/cases/llm_cases.jsonl` (28 case nguồn, giữ cố định).
- Y′: `evaluation/cases/v2_cases.jsonl` (workload dùng để chạy V2).
- Quy trình sinh: `evaluation/generate_v2_cases.py`.
- Corpus thực thi: Elasticsearch index `soc-events`; phải giữ cùng snapshot trong một đợt đo.

Y′ giữ nguyên toàn bộ `id`, `category`, `request`, `expected` và `assisted.editedIntent` của X. Script chỉ bật `scoreFinalResponse` và thêm provenance/hash. Không dùng LLM, random, dữ liệu ngoài, hay giá trị được viết thêm trong lúc sinh. Controlled workflow nằm ở evaluator: gọi `/api/search`, theo confirmation qua `/api/search/confirm` khi cần, rồi chấm response cuối.

## Sinh và kiểm tra Y′

Chạy từ thư mục `demo`:

```powershell
python evaluation\generate_v2_cases.py
python evaluation\generate_v2_cases.py --check
python -m unittest discover -s evaluation -p "test_*.py"
```

`--check` phải chạy trong CI để phát hiện Y′ bị sửa tay hoặc không còn khớp X.

Regression test cũng kiểm tra V1 và V2 có cùng denominator theo ground truth: 17 case `DSL Correctness` và 11 case `Aggregation Correctness`. Nhóm aggregation gồm cả sáu terms aggregation và năm time aggregation; việc backend chọn route/template khác không được làm thay đổi nhóm chấm của case.

## Chạy benchmark V2

```powershell
python evaluation\runner.py `
  --variant workflow-v2 `
  --base-url http://localhost:8080 `
  --cases evaluation\cases\v2_cases.jsonl `
  --require-executions 28 `
  --repeat 1 `
  --warmup 0 `
  --timeout 120 `
  --data-snapshot <soc-events-snapshot-id> `
  --provider-config <provider-model-config-hash> `
  --cache-regime cold `
  --output evaluation\reports\v2_report.md `
  --json-output evaluation\reports\v2_report.json
```

Auto-confirm được bật mặc định. Có thể dùng `--no-auto-confirm` chỉ khi cần debug response đầu; chế độ đó không phải protocol benchmark V2.

## EDA tối thiểu trước khi sửa X

EDA chỉ dùng để xác nhận case có thể thực thi trên `soc-events`: schema/mapping, giá trị phổ biến của field filter/group-by, phân phối thời gian, và số lượng hit khác 0. Nếu cần đổi X, thay đổi phải có lý do từ các thống kê này; sau đó sinh lại Y′. Không thêm case hoặc giá trị chỉ dựa trên phỏng đoán.

## File được giữ

```text
evaluation/
├── cases/
│   ├── llm_cases.jsonl       # X, source of truth
│   └── v2_cases.jsonl        # Y′, generated artifact
├── reports/.gitkeep
├── generate_v2_cases.py
├── evaluator.py
├── metrics.py
├── runner.py
├── test_evaluator.py
└── README.md
```


cd D:\VDT_SE_WEB_2026_Main\demo

# Sinh Y′ từ X
python evaluation\generate_v2_cases.py

# Kiểm tra Y′ có tái lập đúng không
python evaluation\generate_v2_cases.py --check

# Chạy test
python -m unittest discover -s evaluation -p "test_*.py"

# Chạy benchmark V2
python evaluation\runner.py `
  --variant workflow-v2 `
  --base-url http://localhost:8080 `
  --cases evaluation\cases\v2_cases.jsonl `
  --require-executions 28 `
  --repeat 1 `
  --warmup 0 `
  --timeout 120 `
  --data-snapshot soc-events-fixed `
  --provider-config current-model-config `
  --cache-regime cold `
  --output evaluation\reports\v2_report.md `
  --json-output evaluation\reports\v2_report.json
