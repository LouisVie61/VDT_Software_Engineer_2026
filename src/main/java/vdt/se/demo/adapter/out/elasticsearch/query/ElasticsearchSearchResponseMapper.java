package vdt.se.demo.adapter.out.elasticsearch.query;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import vdt.se.demo.domain.model.ExecutionResult;
import vdt.se.demo.domain.model.SearchWarning;
import vdt.se.demo.domain.model.SocEventSchema;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ElasticsearchSearchResponseMapper {

    public ExecutionResult map(JsonNode response) {
        return ExecutionResult.builder()
                .results(extractRows(response))
                .aggregations(extractAggregations(response))
                .totalCount(extractTotalCount(response))
                .warnings(extractWarnings(response))
                .build();
    }

    private List<SearchWarning> extractWarnings(JsonNode response) {
        List<SearchWarning> warnings = new ArrayList<>();
        if (response != null && response.path("timed_out").asBoolean(false)) {
            warnings.add(SearchWarning.builder()
                    .code("ELASTICSEARCH_TIMED_OUT")
                    .message("Elasticsearch timed out; displayed data may be incomplete.")
                    .build());
        }
        int failedShards = response == null ? 0 : response.path("_shards").path("failed").asInt(0);
        if (failedShards > 0) {
            warnings.add(SearchWarning.builder()
                    .code("ELASTICSEARCH_SHARD_FAILURE")
                    .message(failedShards + " Elasticsearch shard(s) failed; displayed data may be incomplete.")
                    .build());
        }
        return warnings;
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

        row.put("id", text(source, SocEventSchema.EVENT_ID, text(hit, "_id", null)));

        for (String field : SocEventSchema.RESPONSE_FIELDS) {
            row.put(field, value(source, field, null));
        }

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
            JsonNode aggregation = entry.getValue();
            List<Map<String, Object>> buckets = extractBuckets(entry.getKey(), aggregation);
            if (buckets.isEmpty()) {
                extractMetric(entry.getKey(), aggregation).forEach(rows::add);
            } else {
                rows.addAll(buckets);
            }
        }
        return rows;
    }

    private List<Map<String, Object>> extractMetric(String aggregationName, JsonNode aggregation) {
        JsonNode value = aggregation == null ? null : aggregation.get("value");
        if (value == null || value.isNull()) {
            return List.of();
        }
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("aggregation", aggregationName);
        row.put("value", value.asDouble());
        JsonNode valueAsString = aggregation.get("value_as_string");
        if (valueAsString != null && !valueAsString.isNull()) {
            row.put("value_as_string", valueAsString.asString());
        }
        return List.of(row);
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
        if (key == null || key.isNull()) {
            return null;
        }

        if (key.isString()) {
            return key.asString();
        }

        if (key.isNumber()) {
            return key.numberValue();
        }

        if (key.isBoolean()) {
            return key.asBoolean();
        }

        if (key.isObject()) {
            Map<String, Object> parts = new LinkedHashMap<>();
            for (Map.Entry<String, JsonNode> entry : key.properties()) {
                parts.put(entry.getKey(), jsonValue(entry.getValue()));
            }
            return parts;
        }

        if (key.isArray()) {
            List<Object> values = new ArrayList<>();
            for (JsonNode item : key) {
                values.add(jsonValue(item));
            }
            return values;
        }

        return key.toString();
    }

    private Object jsonValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }

        if (node.isString()) {
            String text = node.asString();
            return text == null || text.isBlank() ? null : text;
        }

        if (node.isNumber()) {
            return node.numberValue();
        }

        if (node.isBoolean()) {
            return node.asBoolean();
        }

        if (node.isObject()) {
            Map<String, Object> object = new LinkedHashMap<>();
            for (Map.Entry<String, JsonNode> entry : node.properties()) {
                object.put(entry.getKey(), jsonValue(entry.getValue()));
            }
            return object;
        }

        if (node.isArray()) {
            List<Object> array = new ArrayList<>();
            for (JsonNode item : node) {
                array.add(jsonValue(item));
            }
            return array;
        }

        return node.toString();
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

        if (!value.isString()) {
            return value.toString();
        }

        String text = value.asString();
        return text == null || text.isBlank() ? defaultValue : text;
    }

    private Object value(JsonNode node, String field, Object defaultValue) {
        if (node == null || node.isNull()) {
            return defaultValue;
        }

        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return defaultValue;
        }

        if (value.isString()) {
            String text = value.asString();
            return text == null || text.isBlank() ? defaultValue : text;
        }

        if (value.isNumber()) {
            return value.numberValue();
        }

        if (value.isBoolean()) {
            return value.asBoolean();
        }

        if (value.isObject() || value.isArray()) {
            return value;
        }

        return value.toString();
    }

}
