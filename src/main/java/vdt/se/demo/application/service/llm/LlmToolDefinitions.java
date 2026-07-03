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
                {"name":"search_events","description":"Compose a new IQL query or patch the previous query. Use $ref for values from previous results.",
                 "input_schema":{"type":"object","required":["mode"],"properties":{
                   "mode":{"enum":["new","patch"]},"select":{"type":"array","items":{"type":"string"}},
                   "filters":{"type":"array","items":{"$ref":"#/definitions/filter"}},
                   "filter_logic":{"type":"object"},"time_range":{"type":"object"},
                   "group_by":{"type":"array","items":{"type":"object","required":["field"]}},
                   "metrics":{"type":"array","items":{"type":"object","required":["type"]}},
                   "order_by":{"type":"object"},"sort":{"type":"array"},"size":{"type":"integer"},
                   "patch_ops":{"type":"array"}},
                 "definitions":{"filter":{"type":"object","required":["id","field","op","value"],"properties":{
                   "id":{"type":"string"},"field":{"type":"string"},
                   "op":{"enum":["eq","neq","in","not_in","gt","gte","lt","lte","exists","contains"]},
                   "value":{"oneOf":[{"type":["string","number","boolean","array"]},{"type":"object","required":["$ref"]}]}}}}}}
                """);
    }

    public JsonNode askClarification() {
        return mapper.readTree("""
                {"name":"ask_clarification","description":"Use instead of search_events when the request cannot be translated safely without guessing.",
                 "input_schema":{"type":"object","required":["reason","question"],"properties":{
                   "reason":{"enum":["ambiguous_reference","missing_field","unsafe_scope","unclear_intent"]},
                   "question":{"type":"string"},"candidates":{"type":"array","items":{"type":"string"}}}}}
                """);
    }
}
