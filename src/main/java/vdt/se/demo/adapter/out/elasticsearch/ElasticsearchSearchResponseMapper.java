package vdt.se.demo.adapter.out.elasticsearch;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import vdt.se.demo.domain.model.ExecutionResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ElasticsearchSearchResponseMapper {

    public ExecutionResult map(JsonNode response) {
        return new ExecutionResult(
                extractRows(response),
                extractAggregations(response),
                extractTotalCount(response)
        );
    }

    private List<Map<String, Object>> extractRows(JsonNode response) {
        JsonNode hits = path(response, "hits", "hits");
        if (hits == null || !hits.isArray()) {
            return List.of();
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (JsonNode hit : hits) {
            rows.add(sourceToRow(hit));
        }
        return rows;
    }

    private Map<String, Object> sourceToRow(JsonNode hit) {
        JsonNode source = hit == null ? null : hit.get("_source");
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", text(source, "id", text(hit, "_id", null)));
        row.put("timestamp", text(source, "timestamp", null));
        row.put("source", text(source, "source", null));
        row.put("severity", text(source, "severity", null));
        row.put("event_type", text(source, "event_type", null));
        row.put("user", text(source, "user", null));
        row.put("host", text(source, "host", null));
        row.put("ip", text(source, "ip", null));
        row.put("src_ip", text(source, "src_ip", null));
        row.put("dst_ip", text(source, "dst_ip", null));
        row.put("action", text(source, "action", null));
        row.put("message", text(source, "message", null));
        row.put("raw", text(source, "raw", null));
        return row;
    }

    private int extractTotalCount(JsonNode response) {
        JsonNode total = path(response, "hits", "total");
        if (total == null || total.isNull()) {
            return 0;
        }
        if (!total.isObject()) {
            return total.asInt();
        }
        JsonNode value = total.get("value");
        return value == null || value.isNull() ? 0 : value.asInt();
    }

    private List<Map<String, Object>> extractAggregations(JsonNode response) {
        JsonNode aggregations = response == null ? null : response.get("aggregations");
        if (aggregations == null || aggregations.isNull() || !aggregations.isObject()) {
            return List.of();
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map.Entry<String, JsonNode> entry : aggregations.properties()) {
            rows.addAll(extractBuckets(entry.getKey(), entry.getValue()));
        }
        return rows;
    }

    private List<Map<String, Object>> extractBuckets(String aggregationName, JsonNode aggregation) {
        JsonNode buckets = aggregation == null ? null : aggregation.get("buckets");
        if (buckets == null || !buckets.isArray()) {
            return List.of();
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (JsonNode bucket : buckets) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("aggregation", aggregationName);
            row.put("key", bucketKey(bucket));
            row.put("count", intValue(bucket.get("doc_count")));
            rows.add(row);
        }
        return rows;
    }

    private Object bucketKey(JsonNode bucket) {
        JsonNode keyAsString = bucket.get("key_as_string");
        if (keyAsString != null && !keyAsString.isNull()) {
            return keyAsString.asString();
        }
        JsonNode key = bucket.get("key");
        return key == null || key.isNull() ? null : key.asString();
    }

    private JsonNode path(JsonNode root, String first, String second) {
        JsonNode parent = root == null ? null : root.get(first);
        return parent == null || parent.isNull() ? null : parent.get(second);
    }

    private int intValue(JsonNode node) {
        return node == null || node.isNull() ? 0 : node.asInt();
    }

    private String text(JsonNode node, String field, String defaultValue) {
        if (node == null || node.isNull()) {
            return defaultValue;
        }
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return defaultValue;
        }
        String text = value.asString();
        return text == null || text.isBlank() ? defaultValue : text;
    }
}
