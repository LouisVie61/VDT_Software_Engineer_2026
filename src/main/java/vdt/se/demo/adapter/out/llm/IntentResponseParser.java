package vdt.se.demo.adapter.out.llm;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.domain.exception.BadQueryException;
import vdt.se.demo.domain.model.SearchIntent;
import vdt.se.demo.domain.valueObjects.TemplateType;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Component
public class IntentResponseParser {
    private final ObjectMapper objectMapper;

    public IntentResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public SearchIntent parse(String raw) {
        try {
            JsonNode root = objectMapper.readTree(stripFences(raw));
            return SearchIntent.builder()
                    .intent(template(root.get("intent")))
                    .textQuery(text(root, "textQuery"))
                    .filters(filters(root.get("filters")))
                    .groupBy(text(root, "groupBy"))
                    .metric(value(text(root, "metric"), "COUNT"))
                    .topN(root.hasNonNull("topN") ? root.get("topN").asInt() : null)
                    .timeBucket(text(root, "timeBucket"))
                    .timeFrom(text(root.get("timeRange"), "from"))
                    .timeTo(text(root.get("timeRange"), "to"))
                    .overrideIntent(text(root, "overrideIntent"))
                    .overrideReason(text(root, "overrideReason"))
                    .confidenceScores(confidenceScores(root.get("confidenceScores")))
                    .build();
        } catch (Exception e) {
            throw new BadQueryException("LLM intent response is not valid JSON fields", e);
        }
    }

    private TemplateType template(JsonNode node) {
        if (node == null || node.isNull()) {
            return TemplateType.SIMPLE_SEARCH;
        }
        return TemplateType.valueOf(node.asString().trim().toUpperCase(Locale.ROOT));
    }

    private Map<String, String> filters(JsonNode node) {
        Map<String, String> filters = new LinkedHashMap<>();
        if (node == null || !node.isObject()) {
            return filters;
        }
        Iterator<Map.Entry<String, JsonNode>> iterator = node.properties().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            if (entry.getValue() != null && !entry.getValue().isNull() && !entry.getValue().asString().isBlank()) {
                filters.put(entry.getKey(), entry.getValue().asString().trim());
            }
        }
        return filters;
    }

    private Map<String, Double> confidenceScores(JsonNode node) {
        Map<String, Double> scores = new LinkedHashMap<>();
        if (node == null || !node.isObject()) {
            return scores;
        }
        Iterator<Map.Entry<String, JsonNode>> iterator = node.properties().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            if (entry.getValue() != null && entry.getValue().isNumber()) {
                scores.put(entry.getKey(), Math.max(0.0d, Math.min(1.0d, entry.getValue().asDouble())));
            }
        }
        return scores;
    }

    private String stripFences(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.startsWith("```")) {
            value = value.replaceFirst("^```(?:json)?", "").replaceFirst("```$", "").trim();
        }
        return value;
    }

    private String text(JsonNode root, String field) {
        JsonNode node = root == null ? null : root.get(field);
        if (node == null || node.isNull()) {
            return null;
        }
        String value = node.asString();
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String value(String value, String fallback) {
        return value == null ? fallback : value;
    }
}

