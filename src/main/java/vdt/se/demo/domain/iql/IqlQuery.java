package vdt.se.demo.domain.iql;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Locale;
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

    public enum Operator {
        EQ, NEQ, IN, NOT_IN, GT, GTE, LT, LTE, EXISTS, CONTAINS;

        @JsonCreator
        public static Operator fromJson(String value) {
            return parseEnum(Operator.class, value, "operator");
        }

        @JsonValue
        public String toJson() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public record FilterLogic(List<Object> and, List<Object> or, List<Object> not) {}

    public record TimeRange(String field, String from, String to) {
        public TimeRange {
            field = field == null || field.isBlank() ? "timestamp" : field;
        }
    }

    public record GroupBy(String field, Integer size, SampleHits sampleHits) {
        public GroupBy(String field, Integer size) {
            this(field, size, null);
        }

        public int effectiveSize() {
            return size == null ? 10 : size;
        }
    }

    public record SampleHits(Integer size, List<Sort> sort) {
        public SampleHits {
            sort = sort == null ? List.of() : List.copyOf(sort);
        }

        public int effectiveSize() {
            return size == null ? 5 : Math.max(1, Math.min(size, 20));
        }
    }

    public record Metric(MetricType type, String field) {}

    public enum MetricType {
        COUNT, CARDINALITY, AVG, SUM, MIN, MAX;

        @JsonCreator
        public static MetricType fromJson(String value) {
            return parseEnum(MetricType.class, value, "metric type");
        }

        @JsonValue
        public String toJson() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public record OrderBy(OrderTarget target, Integer metricIndex, Direction direction) {}

    public enum OrderTarget {
        METRIC, KEY, COUNT;

        @JsonCreator
        public static OrderTarget fromJson(String value) {
            return parseEnum(OrderTarget.class, value, "order target");
        }

        @JsonValue
        public String toJson() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public record Sort(String field, Direction order) {}

    public enum Direction {
        ASC, DESC;

        @JsonCreator
        public static Direction fromJson(String value) {
            return parseEnum(Direction.class, value, "direction");
        }

        @JsonValue
        public String toJson() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing " + label);
        }

        String normalized = value.trim()
                .replace("-", "_")
                .toUpperCase(Locale.ROOT);

        try {
            return Enum.valueOf(enumClass, normalized);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unsupported " + label + ": " + value, e);
        }
    }
}