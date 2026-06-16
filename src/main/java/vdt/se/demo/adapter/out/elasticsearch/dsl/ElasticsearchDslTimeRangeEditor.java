package vdt.se.demo.adapter.out.elasticsearch.dsl;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.util.Map;

@Component
public class ElasticsearchDslTimeRangeEditor {

    public boolean containsNow(JsonNode node) {
        if (node == null || node.isNull()) {
            return false;
        }
        if (node.isString()) {
            return node.asString().toLowerCase().contains("now");
        }
        if (node.isObject()) {
            for (Map.Entry<String, JsonNode> entry : node.properties()) {
                if (containsNow(entry.getValue())) {
                    return true;
                }
            }
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                if (containsNow(item)) {
                    return true;
                }
            }
        }
        return false;
    }

    public JsonNode withoutTimestampRange(JsonNode dsl) {
        JsonNode copy = dsl.deepCopy();
        removeTimestampRangeFilters(copy);
        return copy;
    }

    public JsonNode withTimestampRange(JsonNode dsl, Instant from, Instant to) {
        JsonNode copy = dsl.deepCopy();
        replaceTimestampRanges(copy, from, to);
        return copy;
    }

    private boolean removeTimestampRangeFilters(JsonNode node) {
        if (node == null || node.isNull()) {
            return false;
        }
        if (isTimestampRange(node)) {
            return true;
        }
        if (node instanceof ObjectNode objectNode) {
            for (Map.Entry<String, JsonNode> entry : objectNode.properties()) {
                removeTimestampRangeFilters(entry.getValue());
            }
        }
        if (node instanceof ArrayNode arrayNode) {
            for (int i = arrayNode.size() - 1; i >= 0; i--) {
                if (removeTimestampRangeFilters(arrayNode.get(i))) {
                    arrayNode.remove(i);
                }
            }
        }
        return false;
    }

    private void replaceTimestampRanges(JsonNode node, Instant from, Instant to) {
        if (node == null || node.isNull()) {
            return;
        }
        if (isTimestampRange(node)) {
            ObjectNode timestamp = (ObjectNode) node.get("range").get("timestamp");
            timestamp.put("gte", from.toString());
            timestamp.put("lte", to.toString());
            return;
        }
        if (node.isObject()) {
            for (Map.Entry<String, JsonNode> entry : node.properties()) {
                replaceTimestampRanges(entry.getValue(), from, to);
            }
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                replaceTimestampRanges(item, from, to);
            }
        }
    }

    private boolean isTimestampRange(JsonNode node) {
        if (!node.isObject()) {
            return false;
        }
        JsonNode range = node.get("range");
        JsonNode timestamp = range == null ? null : range.get("timestamp");
        return timestamp != null && timestamp.isObject();
    }
}
