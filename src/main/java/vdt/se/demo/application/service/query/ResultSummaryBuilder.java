package vdt.se.demo.application.service.query;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.domain.iql.IqlQuery;
import vdt.se.demo.domain.iql.ResultSummary;
import vdt.se.demo.domain.model.ExecutionResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ResultSummaryBuilder {
    static final int MAX_BUCKETS = 20;
    private final ObjectMapper mapper;

    public ResultSummaryBuilder(ObjectMapper mapper) { this.mapper = mapper; }

    public ResultSummary build(IqlQuery query, ExecutionResult result) {
        List<ResultSummary.Bucket> buckets = new ArrayList<>();
        for (Map<String, Object> row : result.aggregations()) {
            if (buckets.size() == MAX_BUCKETS) break;
            Map<String, JsonNode> key = bucketKey(query, row.get("key"));
            if (key.isEmpty()) continue;
            Map<String, Number> metrics = new LinkedHashMap<>();
            number(row.get("count")).ifPresent(value -> metrics.put("count", value));
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                if (entry.getKey().startsWith("metric_"))
                    number(entry.getValue()).ifPresent(value -> metrics.put(entry.getKey(), value));
            }
            buckets.add(new ResultSummary.Bucket(key, metrics));
        }
        Number topMetric = buckets.isEmpty() ? null : buckets.getFirst().metrics().values().stream().findFirst().orElse(null);
        return new ResultSummary(result.totalCount(), query.timeRange(), buckets, topMetric);
    }

    private Map<String, JsonNode> bucketKey(IqlQuery query, Object rawKey) {
        if (rawKey == null || query.groupBy().isEmpty()) return Map.of();
        JsonNode node = mapper.valueToTree(rawKey);
        if (node.isObject()) {
            Map<String, JsonNode> values = new LinkedHashMap<>();
            node.properties().forEach(entry -> values.put(entry.getKey(), entry.getValue()));
            return values;
        }
        return Map.of(query.groupBy().getFirst().field(), node);
    }

    private java.util.Optional<Number> number(Object value) {
        return value instanceof Number number ? java.util.Optional.of(number) : java.util.Optional.empty();
    }
}
