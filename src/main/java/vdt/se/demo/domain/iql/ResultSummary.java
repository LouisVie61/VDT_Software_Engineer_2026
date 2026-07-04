package vdt.se.demo.domain.iql;

import tools.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;

public record ResultSummary(long totalCount, IqlQuery.TimeRange timeRangeUsed,
                            List<Bucket> buckets, Number topMetric) {
    public ResultSummary { buckets = buckets == null ? List.of() : List.copyOf(buckets); }
    public record Bucket(Map<String, JsonNode> key, Map<String, Number> metrics) {}
}
