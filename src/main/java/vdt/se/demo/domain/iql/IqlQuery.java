package vdt.se.demo.domain.iql;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public record IqlQuery(List<String> select, List<FilterCondition> filters, FilterLogic filterLogic,
                       TimeRange timeRange, List<GroupBy> groupBy, List<Metric> metrics,
                       OrderBy orderBy, List<Sort> sort, int size, Map<String, JsonNode> pageAfter,
                       List<Window> windows, List<HavingCondition> having, List<DerivedMetric> derivedMetrics) {
    public IqlQuery {
        select = select == null ? List.of() : List.copyOf(select);
        filters = filters == null ? List.of() : List.copyOf(filters);
        groupBy = groupBy == null ? List.of() : List.copyOf(groupBy);
        metrics = metrics == null ? List.of() : List.copyOf(metrics);
        sort = sort == null ? List.of() : List.copyOf(sort);
        size = size <= 0 ? 50 : size;
        pageAfter = pageAfter == null ? null : Map.copyOf(pageAfter);
        windows = windows == null ? List.of() : List.copyOf(windows);
        having = having == null ? List.of() : List.copyOf(having);
        derivedMetrics = derivedMetrics == null ? List.of() : List.copyOf(derivedMetrics);
    }

    public IqlQuery(List<String> select, List<FilterCondition> filters, FilterLogic filterLogic,
                    TimeRange timeRange, List<GroupBy> groupBy, List<Metric> metrics,
                    OrderBy orderBy, List<Sort> sort, int size, Map<String, JsonNode> pageAfter) {
        this(select, filters, filterLogic, timeRange, groupBy, metrics, orderBy, sort, size, pageAfter,
                List.of(), List.of(), List.of());
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

    public record Window(String name, TimeRange timeRange, List<FilterCondition> filters) {
        public Window {
            filters = filters == null ? List.of() : List.copyOf(filters);
        }
    }

    public record HavingCondition(String metric, String window, ComparisonOp op, Double value) {}

    public enum ComparisonOp {
        EQ, NEQ, GT, GTE, LT, LTE;

        @JsonCreator
        public static ComparisonOp fromJson(String value) {
            return parseEnum(ComparisonOp.class, value, "comparison operator");
        }

        @JsonValue
        public String toJson() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public record DerivedMetric(String name, DerivedMetricType type, MetricRef numerator, MetricRef denominator) {}

    public enum DerivedMetricType {
        RATIO, PERCENT;

        @JsonCreator
        public static DerivedMetricType fromJson(String value) {
            return parseEnum(DerivedMetricType.class, value, "derived metric type");
        }

        @JsonValue
        public String toJson() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public record MetricRef(String metric, String window) {}

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
