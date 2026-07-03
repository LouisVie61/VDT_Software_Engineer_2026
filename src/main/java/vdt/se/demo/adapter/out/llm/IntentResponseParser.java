package vdt.se.demo.adapter.out.llm;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.domain.exception.BadQueryException;
import vdt.se.demo.domain.model.RecurringTime;
import vdt.se.demo.domain.model.SearchIntent;
import vdt.se.demo.domain.model.SemanticSpan;
import vdt.se.demo.domain.valueObjects.TemplateType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
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
                    .recurringTime(recurringTime(root.get("recurringTime")))
                    .overrideIntent(text(root, "overrideIntent"))
                    .overrideReason(text(root, "overrideReason"))
                    .semanticSpans(semanticSpans(root.get("semanticSpans")))
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

    private List<SemanticSpan> semanticSpans(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<SemanticSpan> spans = new ArrayList<>();
        for (JsonNode item : node) {
            SemanticSpan.Kind kind = enumValue(SemanticSpan.Kind.class, text(item, "kind"), null);
            SemanticSpan.Status status = enumValue(SemanticSpan.Status.class, text(item, "status"), SemanticSpan.Status.AMBIGUOUS);
            String text = text(item, "text");
            if (kind != null && text != null) {
                spans.add(SemanticSpan.builder()
                        .kind(kind)
                        .status(status)
                        .text(text)
                        .canonical(text(item, "canonical"))
                        .start(number(item, "start"))
                        .end(number(item, "end"))
                        .build());
            }
        }
        return spans;
    }

    private RecurringTime recurringTime(JsonNode node) {
        if (node == null || !node.isObject()) {
            return null;
        }
        String mode = text(node, "mode");
        Integer month = node.hasNonNull("month") && node.get("month").isNumber() ? node.get("month").asInt() : null;
        if (mode == null && month == null) {
            return null;
        }
        return RecurringTime.builder()
                .mode(mode)
                .month(month)
                .build();
    }

    private <T extends Enum<T>> T enumValue(Class<T> type, String value, T fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
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

    private int number(JsonNode root, String field) {
        JsonNode node = root == null ? null : root.get(field);
        return node != null && node.isNumber() ? node.asInt() : -1;
    }

    private String value(String value, String fallback) {
        return value == null ? fallback : value;
    }
}

