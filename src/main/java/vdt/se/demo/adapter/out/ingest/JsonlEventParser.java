package vdt.se.demo.adapter.out.ingest;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.domain.model.SocEvent;
import vdt.se.demo.domain.service.SocEventMapper;

import java.util.Map;

public class JsonlEventParser {

    private final ObjectMapper objectMapper;
    private final SocEventMapper eventMapper;

    public JsonlEventParser(ObjectMapper objectMapper, SocEventMapper eventMapper) {
        this.objectMapper = objectMapper;
        this.eventMapper = eventMapper;
    }

    public SocEvent parse(String line) {
        JsonNode node = objectMapper.readTree(line);
        Map<String, Object> fields = objectMapper.convertValue(node, new TypeReference<>() {
        });
        return eventMapper.fromJson(fields, node.toString());
    }
}
