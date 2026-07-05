package vdt.se.demo.application.service.validation;

import vdt.se.demo.domain.iql.IqlQuery;
import vdt.se.demo.domain.model.SocEventSchema;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class SchemaRegistry {
    private static final int MAX_GROUPS = 3;
    private static final int MAX_BUCKET_SIZE = 1000;
    private static final int MAX_RESULT_SIZE = 500;

    public ValidationResult validate(IqlQuery query) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        if (query == null) return new ValidationResult(List.of("Query is required"), warnings);
        query.select().forEach(field -> allowed(field, "select", errors));
        Set<String> ids = new HashSet<>();
        for (IqlQuery.FilterCondition filter : query.filters()) {
            if (filter.id() == null || filter.id().isBlank() || !ids.add(filter.id())) errors.add("Filter ids must be non-blank and unique");
            allowed(filter.field(), "filter", errors);
            if (!SocEventSchema.FILTERABLE_FIELDS.contains(filter.field()) && !SocEventSchema.FULL_TEXT_FIELDS.contains(filter.field()))
                errors.add("Field is not filterable: " + filter.field());
            if (filter.op() == IqlQuery.Operator.CONTAINS && !SocEventSchema.FULL_TEXT_FIELDS.contains(filter.field()))
                errors.add("contains is only allowed for full-text fields");
            if (filter.op() != IqlQuery.Operator.EXISTS && (filter.value() == null || filter.value().isNull()))
                errors.add("Filter value is required: " + filter.id());
        }
        if (query.groupBy().size() > MAX_GROUPS) errors.add("At most " + MAX_GROUPS + " group_by fields are allowed");
        query.groupBy().forEach(group -> {
            allowed(group.field(), "group_by", errors);
            if (!SocEventSchema.GROUPABLE_FIELDS.contains(group.field())) errors.add("Field is not groupable: " + group.field());
            if (group.effectiveSize() < 1 || group.effectiveSize() > MAX_BUCKET_SIZE) errors.add("Invalid bucket size for: " + group.field());
        });
        query.metrics().forEach(metric -> validateMetric(metric, errors));
        validateWindows(query, errors, warnings);
        validatePipelineAggregations(query, errors);
        if (query.size() > MAX_RESULT_SIZE) errors.add("Result size exceeds " + MAX_RESULT_SIZE);
        validateTimeRange(query.timeRange(), errors, warnings);
        if (query.pageAfter() != null && query.groupBy().isEmpty()) errors.add("page_after requires group_by");
        if (query.pageAfter() != null && query.orderBy() != null
                && query.orderBy().target() == IqlQuery.OrderTarget.METRIC && query.groupBy().size() > 1)
            errors.add("Composite pagination with metric bucket ordering is not supported");
        if (!query.groupBy().isEmpty() && !query.sort().isEmpty()) warnings.add("hits sort is ignored when group_by is present");
        return new ValidationResult(errors, warnings);
    }

    private void validateWindows(IqlQuery query, List<String> errors, List<String> warnings) {
        Set<String> names = new HashSet<>();
        for (IqlQuery.Window window : query.windows()) {
            if (!validIdentifier(window.name()) || !names.add(window.name())) {
                errors.add("Window names must be unique safe identifiers");
            }
            validateTimeRange(window.timeRange(), errors, warnings);
            Set<String> filterIds = new HashSet<>();
            for (IqlQuery.FilterCondition filter : window.filters()) {
                if (filter.id() == null || filter.id().isBlank() || !filterIds.add(filter.id())) {
                    errors.add("Window filter ids must be non-blank and unique");
                }
                allowed(filter.field(), "window filter", errors);
                if (!SocEventSchema.FILTERABLE_FIELDS.contains(filter.field()) && !SocEventSchema.FULL_TEXT_FIELDS.contains(filter.field())) {
                    errors.add("Field is not filterable in window: " + filter.field());
                }
            }
        }
    }

    private void validatePipelineAggregations(IqlQuery query, List<String> errors) {
        if (!query.having().isEmpty() && query.groupBy().isEmpty()) {
            errors.add("having requires group_by");
        }
        if (!query.derivedMetrics().isEmpty() && query.groupBy().isEmpty()) {
            errors.add("derived_metrics requires group_by; use named window/filter counts for top-level percentages");
        }
        if (!query.having().isEmpty()) {
            query.groupBy().forEach(group -> {
                if (group.effectiveSize() < MAX_BUCKET_SIZE
                        && !group.field().startsWith("timestamp_")
                        && query.groupBy().size() == 1) {
                    errors.add("having on terms group_by requires size " + MAX_BUCKET_SIZE + " to avoid truncated buckets: " + group.field());
                }
            });
        }
        query.having().forEach(condition -> {
            validateMetricRef(condition.metric(), condition.window(), query, errors);
            if (condition.op() == null) errors.add("having op is required");
            if (condition.value() == null || !Double.isFinite(condition.value())) errors.add("having value must be a finite number");
        });
        query.derivedMetrics().forEach(derived -> {
            if (!validIdentifier(derived.name())) errors.add("derived metric name must be a safe identifier");
            if (derived.type() == null) errors.add("derived metric type is required");
            validateMetricRef(derived.numerator() == null ? null : derived.numerator().metric(),
                    derived.numerator() == null ? null : derived.numerator().window(), query, errors);
            validateMetricRef(derived.denominator() == null ? null : derived.denominator().metric(),
                    derived.denominator() == null ? null : derived.denominator().window(), query, errors);
        });
    }

    private void validateMetricRef(String metric, String window, IqlQuery query, List<String> errors) {
        if (!"count".equals(metric)) {
            errors.add("Only count is currently supported in pipeline metric references");
        }
        if (window != null && !window.isBlank()
                && query.windows().stream().noneMatch(candidate -> window.equals(candidate.name()))) {
            errors.add("Unknown window in pipeline metric reference: " + window);
        }
    }

    private boolean validIdentifier(String value) {
        return value != null && value.matches("[A-Za-z][A-Za-z0-9_]{0,63}");
    }

    private void validateMetric(IqlQuery.Metric metric, List<String> errors) {
        if (metric == null) {
            errors.add("Metric is required");
            return;
        }

        if (metric.type() == null) {
            errors.add("Metric type is required");
            return;
        }

        if (metric.type() == IqlQuery.MetricType.COUNT) {
            return;
        }

        if (metric.field() == null || metric.field().isBlank()) {
            errors.add(metric.type() + " requires a field");
            return;
        }

        allowed(metric.field(), "metric", errors);
        if (metric.type() != IqlQuery.MetricType.CARDINALITY
                && !SocEventSchema.NUMERIC_METRIC_FIELDS.contains(metric.field())) {
            errors.add(metric.type() + " requires a numeric field: " + metric.field());
        }
    }

    private void validateTimeRange(IqlQuery.TimeRange range, List<String> errors, List<String> warnings) {
        if (range == null) return;
        if (!SocEventSchema.TIMESTAMP.equals(range.field())) errors.add("Time range field must be timestamp");
        if (range.from() == null || range.to() == null) return;
        try {
            if (!isDateMath(range.from()) && !isDateMath(range.to())
                    && !Instant.parse(range.from()).isBefore(Instant.parse(range.to()))) errors.add("Time range from must be before to");
        } catch (RuntimeException ignored) { errors.add("Time range must use ISO-8601 or now-relative values"); }
    }

    private boolean isDateMath(String value) {
        return value != null && value.startsWith("now");
    }

    private void allowed(String field, String usage, List<String> errors) {
        if (field == null || !SocEventSchema.FIELD_WHITELIST.contains(field)) errors.add("Unknown " + usage + " field: " + field);
    }
}
