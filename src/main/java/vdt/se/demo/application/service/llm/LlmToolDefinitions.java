package vdt.se.demo.application.service.llm;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

public final class LlmToolDefinitions {
    private final ObjectMapper mapper;

    public LlmToolDefinitions(ObjectMapper mapper) { this.mapper = mapper; }

    public List<JsonNode> all() { return List.of(searchEvents(), askClarification()); }

    public JsonNode searchEvents() {
        return mapper.readTree("""
                {"name":"search_events","description":"EN: Search, filter, sort, count, or group SOC events by composing a new IQL query or patching the previous query. VI: Tìm kiếm, lọc, sắp xếp, đếm hoặc nhóm sự kiện SOC bằng truy vấn IQL mới hoặc bản vá truy vấn trước. Encode filter and patch values as JSON text; use a JSON object containing $ref for prior-result references. For count, omit field; other metrics require field.",
                "input_schema":{"type":"object","required":["mode"],"properties":{
                  "mode":{"type":"string","enum":["new","patch"],"description":"EN: new request or refinement of previous query. VI: yêu cầu mới hoặc điều chỉnh truy vấn trước."},
                  "select":{"type":"array","description":"EN: Fields returned for event rows. VI: Các trường trả về cho từng sự kiện.","items":{"type":"string"}},

                  "filters":{"type":"array","items":{"type":"object","required":["id","field","op","value"],"properties":{
                    "id":{"type":"string"},
                    "field":{"type":"string","description":"EN: Canonical schema field. VI: Tên trường schema chuẩn bằng tiếng Anh."},
                    "op":{"type":"string","enum":["eq","neq","in","not_in","gt","gte","lt","lte","exists","contains"]},
                    "value":{"type":"string","description":"JSON-encoded scalar, array, or prior-result reference object"}
                  }}},

                  "time_range":{"type":"object","description":"EN: Event time window. VI: Khoảng thời gian sự kiện.","required":["field"],"properties":{
                    "field":{"type":"string"},
                    "from":{"type":"string"},
                    "to":{"type":"string"}
                  }},

                  "group_by":{"type":"array","description":"EN: Bucket dimensions such as source or timestamp_day. VI: Chiều nhóm như nguồn hoặc ngày.","items":{"type":"object","required":["field"],"properties":{
                    "field":{"type":"string"},
                    "size":{"type":"integer","minimum":1,"maximum":1000},
                    "sample_hits":{"type":"object","description":"EN: Optional preview of matching events per bucket. VI: Xem trước sự kiện mẫu trong mỗi nhóm.","properties":{
                      "size":{"type":"integer","minimum":1,"maximum":20},
                      "sort":{"type":"array","items":{"type":"object","required":["field","order"],"properties":{
                        "field":{"type":"string"},"order":{"type":"string","enum":["asc","desc"]}
                      }}}
                    }}
                  }}},

                  "metrics":{"type":"array","description":"EN: Aggregated measurements. VI: Các phép đo tổng hợp.","items":{"type":"object","required":["type"],"properties":{
                    "type":{"type":"string","enum":["count","cardinality","avg","sum","min","max"]},
                    "field":{"anyOf":[{"type":"string"},{"type":"null"}],"description":"Required for cardinality, avg, sum, min, max. For count, omit this field or set it to null."}
                  }}},

                  "order_by":{"type":"object","required":["target","direction"],"properties":{
                    "target":{"type":"string","enum":["metric","key","count"]},
                    "metric_index":{"type":"integer","minimum":0},
                    "direction":{"type":"string","enum":["asc","desc"]}
                  }},

                  "sort":{"type":"array","items":{"type":"object","required":["field","order"],"properties":{
                    "field":{"type":"string"},
                    "order":{"type":"string","enum":["asc","desc"]}
                  }}},

                  "size":{"type":"integer"},

                  "patch_ops":{"type":"array","items":{"type":"object","required":["op"],"properties":{
                    "op":{"type":"string","enum":["add_filter","remove_filter","replace_filter","set_group_by","clear_group_by","set_time_range","set_metrics","set_sort","set_size"]},
                    "filter_id":{"type":"string"},
                    "value":{"type":"string","description":"JSON-encoded patch value"}
                  }}}
                }}}
                """.replace('\u01af', ','));
    }

    public JsonNode askClarification() {
        return mapper.readTree("""
                {"name":"ask_clarification","description":"EN: Ask only for information that is genuinely missing and prevents a safe query; never repeat or translate the user's request. VI: Chỉ hỏi thông tin thực sự còn thiếu khiến không thể tạo truy vấn an toàn; không lặp lại hoặc dịch lại yêu cầu của người dùng. Ask in the user's dominant language.",
                 "input_schema":{"type":"object","required":["reason","question"],"properties":{
                   "reason":{"type":"string","enum":["ambiguous_reference","missing_field","unsafe_scope","unclear_intent"]},
                   "question":{"type":"string","description":"EN: Targeted question for missing information. VI: Câu hỏi cụ thể về thông tin còn thiếu, cùng ngôn ngữ với người dùng."},
                   "candidates":{"type":"array","description":"EN: Optional safe choices. VI: Các lựa chọn an toàn nếu có.","items":{"type":"string"}}}}}
                """);
    }
}
