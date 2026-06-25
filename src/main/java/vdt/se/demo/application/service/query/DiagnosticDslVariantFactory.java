package vdt.se.demo.application.service.query;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.Map;

public class DiagnosticDslVariantFactory {
    private final ObjectMapper objectMapper;

    public DiagnosticDslVariantFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public JsonNode countDsl(JsonNode dsl) {
        ObjectNode root = objectMapper.createObjectNode();
        root.set("query", dsl.path("query").deepCopy());
        return root;
    }

    public JsonNode timeRangeOnly(JsonNode dsl) {
        return filteredDsl(dsl, FilterMode.TIME_ONLY, Map.of());
    }

    public JsonNode coreFiltersOnly(JsonNode dsl) {
        return filteredDsl(dsl, FilterMode.CORE_FILTERS_ONLY, Map.of());
    }

    public JsonNode withoutLowConfidenceFilters(JsonNode dsl, Map<String, Double> confidenceScores) {
        return filteredDsl(dsl, FilterMode.REMOVE_LOW_CONFIDENCE, confidenceScores);
    }

    public JsonNode withoutFullText(JsonNode dsl) {
        JsonNode copy = dsl.deepCopy();
        ArrayNode must = must(copy);
        if (must != null) {
            must.removeAll();
            must.add(objectMapper.createObjectNode().putObject("match_all"));
        }
        return copy;
    }

    public JsonNode withoutTimeRange(JsonNode dsl) {
        return filteredDsl(dsl, FilterMode.NO_TIME, Map.of());
    }

    private JsonNode filteredDsl(JsonNode dsl, FilterMode mode, Map<String, Double> confidenceScores) {
        JsonNode copy = withoutFullText(dsl);
        ArrayNode filter = filter(copy);
        if (filter == null) {
            return copy;
        }
        ArrayNode next = objectMapper.createArrayNode();
        for (JsonNode clause : filter) {
            String field = fieldName(clause);
            boolean time = "timestamp".equals(field);
            boolean lowConfidence = confidenceScores.getOrDefault(field, 1.0d) < 0.70d;
            if (mode == FilterMode.TIME_ONLY && time) {
                next.add(clause);
            } else if (mode == FilterMode.CORE_FILTERS_ONLY && !time) {
                next.add(clause);
            } else if (mode == FilterMode.REMOVE_LOW_CONFIDENCE && !lowConfidence) {
                next.add(clause);
            } else if (mode == FilterMode.NO_TIME && !time) {
                next.add(clause);
            }
        }
        filter.removeAll();
        filter.addAll(next);
        return copy;
    }

    private ArrayNode must(JsonNode dsl) {
        JsonNode node = dsl.path("query").path("bool").path("must");
        return node instanceof ArrayNode arrayNode ? arrayNode : null;
    }

    private ArrayNode filter(JsonNode dsl) {
        JsonNode node = dsl.path("query").path("bool").path("filter");
        return node instanceof ArrayNode arrayNode ? arrayNode : null;
    }

    private String fieldName(JsonNode clause) {
        JsonNode term = clause.path("term");
        if (term.isObject() && term.properties().iterator().hasNext()) {
            return term.properties().iterator().next().getKey();
        }
        JsonNode range = clause.path("range");
        if (range.isObject() && range.properties().iterator().hasNext()) {
            return range.properties().iterator().next().getKey();
        }
        return "";
    }

    private enum FilterMode {
        TIME_ONLY,
        CORE_FILTERS_ONLY,
        REMOVE_LOW_CONFIDENCE,
        NO_TIME
    }
}
