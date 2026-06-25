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

## Chon template

### Khi nao `SIMPLE_SEARCH`

Dung `SIMPLE_SEARCH` khi cau hoi chinh la tim/liet ke/kiem tra event:

- Cac cum nhu `show`, `list`, `find`, `get`, `tim`, `cho toi`, `hien thi`, `lay`.
- Co filter truc tiep nhu `severity`, `event_type`, `action`, `user`, `host`, `ip`, `source`.
- Co time range nhung van la search hit page, vi du "high alerts in first week of June".
- Co free-text co nghia de search tren `message`, vi du `connection timeout error`.
- Khong co yeu cau ro rang ve top/count/group-by/trend/timeline.

Expectation nen co:

- `selectedTemplate: "SIMPLE_SEARCH"`.
- `chartType: "table"` neu can assert chart.
- `generatedDslNotContains: ["\"aggs\""]` neu case can dam bao khong bi route sang aggregation.
- Neu execute that su, assert filter/range/textQuery can thiet va `requiresExecutionEvidence=true`.

### Khi nao `TERMS_AGGREGATION`

Dung `TERMS_AGGREGATION` khi cau hoi yeu cau dem/thong ke/phan bo/top theo mot field cu the:

- Cac cum nhu `top`, `count by`, `group by`, `statistics by`, `distribution by`, `thong ke ... theo`.
- Co group field ro rang: `top 10 IP`, `top users`, `by source`, `by severity`, `by event type`, `by action`, `by host`.
- Metric mac dinh la count; benchmark nen ky vong terms aggregation, bucket list, va chart bar/pie tuy topN.

Expectation nen co:

- `selectedTemplate: "TERMS_AGGREGATION"`.
- `generatedDslContains: ["\"terms\""]`.
- `generatedDslPath.exists: ["aggs"]`.
- Assert `generatedDsl.aggs.top_values.terms.field` va `generatedDsl.aggs.top_values.terms.size` khi co Top N ro rang.
- `chartType: "bar_chart"` cho Top N co N; `pie_chart` chi nen assert neu contract case thuc su can.

### Khi nao `TIME_AGGREGATION`

Dung `TIME_AGGREGATION` khi cau hoi yeu cau xu huong theo thoi gian:

- Cac cum nhu `trend`, `timeline`, `over time`, `per hour`, `per day`, `per week`, `moi ngay`, `theo gio`, `theo ngay`, `theo quy`.
- Bucket hop le hien tai: `1h`, `1d`, `1w`; DSL co the dung `fixed_interval` hoac `calendar_interval` voi week/quarter.
- `groupBy` thuong de `null` tru khi analyst noi ro vua theo thoi gian vua theo field.

Expectation nen co:

- `selectedTemplate: "TIME_AGGREGATION"`.
- `chartType: "line_chart"` khi execute; mot so confirmation response co the van tra `table`, chi assert chart khi case da on dinh.
- `generatedDslContains: ["\"date_histogram\"", "\"range\":{\"timestamp\""]` neu co time range.
- Neu confidence thap hoac time expression mo ho, assert confirmation thay vi DSL.

## Guard va confirmation

### Khi nao `GROUP_BY_REQUIRED`

Dung `GROUP_BY_REQUIRED` khi intent la aggregation dem/thong ke/top/grouping nhung khong co field nhom an toan:

- "thong ke cac loi", "count all errors", "statistics alerts" khong noi `by <field>`.
- Khong duoc tu gan field gan nhat chi vi query co tu `loi`, `error`, `alert`.
- Neu case test ambiguity nay, assert:
  - `needsConfirmation: true`
  - `responseContains: ["GROUP_BY_REQUIRED"]`
  - `nonEmptyFields: ["confirmation.confirmationId"]`
  - `allowZeroResults: true`

Neu cau hoi co field co the resolve duoc bang tu khoa ro rang (`ip`, `user`, `host`, `action`, `source`, `severity`, `event type`) thi khong gan `GROUP_BY_REQUIRED`.

### Khi nao `TOP_N_REQUIRED`

Dung `TOP_N_REQUIRED` khi cau hoi co group field ro rang nhung thieu lua chon bucket size va contract case muon bat buoc analyst xac nhan kich thuoc bucket:

- "count events by source".
- "group by severity".
- "statistics by host" neu khong noi top bao nhieu.
- `topN=0` la mot gia tri hop le voi nghia "khong ap Top N cu the tu user"; khi execute, backend duoc dung cap ky thuat mac dinh cho `terms.size` de tranh query bucket vo han.
- Khong duoc dien giai `topN=0` thanh `terms.size=1`.

Expectation nen co:

- `needsConfirmation: true`
- `selectedTemplate: "TERMS_AGGREGATION"`
- `responseContains: ["TOP_N_REQUIRED"]`
- `nonEmptyFields: ["confirmation.confirmationId"]`
- `allowZeroResults: true`

Neu cau hoi co N ro rang (`top 5 users`, `top 10 IP`) thi execute duoc neu cac guard khac pass. Neu case kiem `topN=0` sau confirmation, assert selection/DSL size la cap mac dinh hop le, khong assert `TOP_N_REQUIRED`.

### Khi nao phai confirmation

Assert `needsConfirmation=true` khi mot trong cac dieu kien sau la y do chinh cua case:

- Confidence thap: bat ky confidence score quan trong duoi nguong backend, dac biet free-text/temporal/filter mapping mo.
- `GROUP_BY_REQUIRED` hoac `TOP_N_REQUIRED`.
- `overrideIntent` ton tai, vi LLM dang sua routing hint.
- Query co temporal expression mo: `quy 2` khong co nam, `theo quy` khong co nam/bucket ro, `gan day` neu khong resolve duoc an toan.
- Query residual/free-text co rui ro map sai nghia SOC: `suspicious activity`, `malware beacon activity`, `lateral movement` khi khong co exact known event_type.
- Query co explicit API filter nhung cau hoi tu nhien them y nghia khong chac chan, vi du source/severity/user/host/ip filter ket hop residual text mo.
- Follow-up ngan/cut ngon ma khong du context chac chan.

Confirmation response duoc xem la chua execute:

- Khong bat buoc `totalCount > 0`.
- Khong nen assert chi tiet `generatedDsl`.
- Nen assert `confirmation.confirmationId`, `warnings`, `selectedTemplate`, `page`, `pageSize` neu relevant.
- Neu request co explicit filters va response la confirmation, assert `confirmation.requestFilters` giu cac filter do. Hien contract confirmation dang expose `from`, `to`, `severity`, `eventType`, `user`, `host`, `ip`.

## Field policy

### Field duoc filter/query structured

Structured filters chi nen assert tren cac field:

- `source`
- `severity`
- `event_type`
- `action`
- `user`
- `host`
- `ip`

Time range chi dung `timestamp` trong `range`.

### Field duoc group

`TERMS_AGGREGATION` chi duoc group theo:

- `source`
- `severity`
- `event_type`
- `action`
- `user`
- `host`
- `ip`

`timestamp` khong phai terms group field trong contract hien tai; dung `TIME_AGGREGATION`/`date_histogram` neu can nhom theo thoi gian.

### Field khong duoc query raw

- `raw`: khong query, khong filter, khong aggregate. Luon them `"\"raw\""` vao `generatedDslNotContains` cho case free-text/LLM language neu muon bat rule nay.
- `message`: chi duoc search free-text bang `simple_query_string`; khong dung lam term filter hoac terms aggregation.
- `metadata` va unknown fields: khong filter, khong group, khong assert DSL tren cac field nay.
- Command words va time words khong duoc dua vao textQuery: `show`, `list`, `logs`, `events`, `cho toi`, `hom nay`, `last 24h`, `Q1`, `nam 2024`.

## Cac dieu can thiet khi viet case

- Moi case phai noi ro hanh vi chinh trong `description`: execute search, require confirmation, resolve groupBy, reject missing Top N, hay handle temporal ambiguity.
- Case execute nen co evidence: `requiresExecutionEvidence=true` hoac `minTotalCount`.
- Case confirmation nen co `allowZeroResults=true` va khong assert execution evidence.
- Case confirmation co explicit request filters nen assert `confirmation.requestFilters.<field>` de dam bao confirm lan sau khong mat filter.
- Aggregation execute nen assert ca template, DSL aggregation path, bucket size/field neu co the.
- Ambiguity case chi dung `anyOf` khi ca hai nhanh deu an toan; nhanh execute phai co evidence va khong duoc query `raw`.
- Explicit API filters co uu tien cao hon tu trong question. Neu request co `severity:"critical"` ma question noi `low`, assert DSL dung `critical` va `generatedDslNotContains` gia tri conflict.
- Pagination la contract: neu request co `page`/`pageSize`, assert response giu dung gia tri; voi execution co the assert `generatedDsl.size`.
- Dung `generatedDslPath` khi can kiem field JSON chinh xac; dung `generatedDslContains` cho fragment on dinh; tranh assert vao summary LLM.

## Khuyen nghi

- Moi case chi nen kiem mot hanh vi chinh.
- Uu tien assert tren contract on dinh: status, field response, DSL fragments, pagination, confirmation.
- Query da execute thanh cong phai co evidence: `totalCount > 0` va co `results` hoac `aggregations`. Neu khong, benchmark se fail de tranh false positive.
- Confirmation response duoc xem la chua execute, nen khong bat buoc co result count va khong nen assert chi tiet `generatedDsl`.
- Neu case cho phep ca hai nhanh confirmation/execution, dat DSL assertions trong nhanh `anyOf` co `needsConfirmation=false`.
- Aggregation query hien tai co the tra ve ca hits page (`generatedDsl.size`, `sort`, `results`) va aggregation buckets. Khong mac dinh assert `size=0` hay `sort` khong ton tai tru khi contract do duoc yeu cau rieng.
- `groupBy`/chart bucket khong phai raw-result filter. `results` la page hit theo query/filter goc, nen co the chua nhieu gia tri severity/event_type/user khac nhau tru khi request co explicit filter tren field do.
- Tranh assert vao summary tu LLM neu provider co the thay doi cau chu.
- Voi ambiguity case that su, uu tien assert `needsConfirmation=true` va `confirmation.confirmationId` thay vi cho phep he thong am tham chon template gan nhat.
- Chi dung `anyOf` khi cac nhanh deu la hanh vi an toan. Khong them nhanh "execute simple search" cho query thieu field nhom, thieu nam/quy, hoac thieu thong tin bat buoc de lap DSL dang tin.
- Nen co it nhat mot control case khong ambiguous trong cung file de tranh benchmark qua de, vi du query co field grouping ro rang nhu `Top 10 IP`.
