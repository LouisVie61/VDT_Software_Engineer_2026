# Annotation Guideline

Moi dong trong file `cases/*.jsonl` la mot JSON object doc lap.

## Schema

```json
{
  "id": "soc-001",
  "category": "soc_nl2plan",
  "description": "High severity authentication events",
  "request": {
    "question": "show high severity auth events",
    "page": 0,
    "pageSize": 50,
    "severity": "high",
    "eventType": "auth"
  },
  "expected": {
    "status": 200,
    "needsConfirmation": false,
    "selectedTemplate": "SIMPLE_SEARCH",
    "generatedDslContains": [
      "\"severity\":\"high\"",
      "\"event_type\":\"auth\""
    ],
    "responseFields": ["id", "nlQuery", "generatedDsl", "totalCount", "results"],
    "equals": {
      "page": 0,
      "pageSize": 50,
      "generatedDsl.size": 50
    },
    "minTotalCount": 1
  },
  "thresholds": {
    "max_latency_ms": 3000
  }
}
```

## Nguyen tac gan nhan

- `id`: duy nhat, ngan gon, co prefix theo nhom case.
- `category`: dung mot trong cac nhom: `soc_nl2plan`, `temporal`, `residual`, `ambiguity`, `api_contract`.
- `description`: noi ro y do case dang kiem chung.
- `request`: payload gui truc tiep vao `POST /api/search`.
- `expected.status`: HTTP status mong doi. Mac dinh nen la `200`.
- `expected.responseFields`: cac field bat buoc trong JSON response.
- `expected.generatedDslContains`: cac chuoi bat buoc xuat hien trong JSON string cua `generatedDsl`.
- `expected.generatedDslNotContains`: cac chuoi khong duoc xuat hien trong `generatedDsl`.
- `expected.generatedDslAnyContains`: danh sach cac nhom fragment; moi nhom pass neu it nhat mot fragment xuat hien.
- `expected.equals`: map field path -> expected value. Vi du `"page": 0`, `"generatedDsl.size": 50`.
- `expected.fieldIn`: map field path -> danh sach gia tri chap nhan duoc. Dung cho case co nhieu nhanh hop le.
- `expected.anyOf`: danh sach cac nhanh expectation. Case pass neu it nhat mot nhanh pass. Dung tiet che cho ambiguity case co nhieu cach xu ly an toan.
- `expected.nonEmptyFields`: cac field path bat buoc co gia tri khac rong.
- `expected.minTotalCount` / `expected.maxTotalCount`: rang buoc tong so ket qua neu co.
- `expected.allowZeroResults`: mac dinh la `false`. Chi dat `true` khi case co chu dich kiem empty-result workflow.
- `expected.requiresExecutionEvidence`: dung trong mot nhanh `anyOf` de bat buoc nhanh do co `totalCount > 0` va co `results` hoac `aggregations`.
- `thresholds.max_latency_ms`: nguong latency moi request. Dung nguong rong cho moi truong local/CI chua toi uu.

## Khuyen nghi

- Moi case chi nen kiem mot hanh vi chinh.
- Uu tien assert tren contract on dinh: status, field response, DSL fragments, pagination, confirmation.
- Query da execute thanh cong phai co evidence: `totalCount > 0` va co `results` hoac `aggregations`. Neu khong, benchmark se fail de tranh false positive.
- Confirmation response duoc xem la chua execute, nen khong bat buoc co result count va khong nen assert chi tiet `generatedDsl`.
- Neu case cho phep ca hai nhanh confirmation/execution, dat DSL assertions trong nhanh `anyOf` co `needsConfirmation=false`.
- Aggregation query hien tai co the tra ve ca hits page (`generatedDsl.size`, `sort`, `results`) va aggregation buckets. Khong mac dinh assert `size=0` hay `sort` khong ton tai tru khi contract do duoc yeu cau rieng.
- Tranh assert vao summary tu LLM neu provider co the thay doi cau chu.
- Voi ambiguity case that su, uu tien assert `needsConfirmation=true` va `confirmation.confirmationId` thay vi cho phep he thong am tham chon template gan nhat.
- Chi dung `anyOf` khi cac nhanh deu la hanh vi an toan. Khong them nhanh "execute simple search" cho query thieu field nhom, thieu nam/quy, hoac thieu thong tin bat buoc de lap DSL dang tin.
- Nen co it nhat mot control case khong ambiguous trong cung file de tranh benchmark qua de, vi du query co field grouping ro rang nhu `Top 10 IP`.
