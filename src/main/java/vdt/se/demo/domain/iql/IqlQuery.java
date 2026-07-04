package vdt.se.demo.domain.iql;

import tools.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;

public record IqlQuery(List<String> select, List<FilterCondition> filters, FilterLogic filterLogic,
                       TimeRange timeRange, List<GroupBy> groupBy, List<Metric> metrics,
                       OrderBy orderBy, List<Sort> sort, int size, Map<String, JsonNode> pageAfter) {
    public IqlQuery {
        select = select == null ? List.of() : List.copyOf(select);
        filters = filters == null ? List.of() : List.copyOf(filters);
        groupBy = groupBy == null ? List.of() : List.copyOf(groupBy);
        metrics = metrics == null ? List.of() : List.copyOf(metrics);
        sort = sort == null ? List.of() : List.copyOf(sort);
        size = size <= 0 ? 50 : size;
        pageAfter = pageAfter == null ? null : Map.copyOf(pageAfter);
    }
    public record FilterCondition(String id, String field, Operator op, JsonNode value) {}
    public enum Operator { EQ, NEQ, IN, NOT_IN, GT, GTE, LT, LTE, EXISTS, CONTAINS }
    public record FilterLogic(List<Object> and, List<Object> or, List<Object> not) {}
    public record TimeRange(String field, String from, String to) {
        public TimeRange { field = field == null || field.isBlank() ? "timestamp" : field; }
    }
    public record GroupBy(String field, Integer size) {
        public int effectiveSize() { return size == null ? 10 : size; }
    }
    public record Metric(MetricType type, String field) {}
    public enum MetricType { COUNT, CARDINALITY, AVG, SUM, MIN, MAX }
    public record OrderBy(OrderTarget target, Integer metricIndex, Direction direction) {}
    public enum OrderTarget { METRIC, KEY, COUNT }
    public record Sort(String field, Direction order) {}
    public enum Direction { ASC, DESC }
}
