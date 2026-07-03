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
            if (!SocEventSchema.FILTERABLE_FIELDS.contains(filter.field()) && !SocEventSchema.MESSAGE.equals(filter.field()))
                errors.add("Field is not filterable: " + filter.field());
            if (filter.op() == IqlQuery.Operator.CONTAINS && !SocEventSchema.MESSAGE.equals(filter.field()))
                errors.add("contains is only allowed for message");
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
        if (query.size() > MAX_RESULT_SIZE) errors.add("Result size exceeds " + MAX_RESULT_SIZE);
        validateTimeRange(query.timeRange(), errors, warnings);
        if (query.pageAfter() != null && query.groupBy().isEmpty()) errors.add("page_after requires group_by");
        if (query.pageAfter() != null && query.orderBy() != null
                && query.orderBy().target() == IqlQuery.OrderTarget.METRIC && query.groupBy().size() > 1)
            errors.add("Composite pagination with metric bucket ordering is not supported");
        if (!query.groupBy().isEmpty() && !query.sort().isEmpty()) warnings.add("hits sort is ignored when group_by is present");
        return new ValidationResult(errors, warnings);
    }

    private void validateMetric(IqlQuery.Metric metric, List<String> errors) {
        if (metric.type() == null) { errors.add("Metric type is required"); return; }
        if (metric.type() != IqlQuery.MetricType.COUNT) {
            allowed(metric.field(), "metric", errors);
            if (metric.field() == null || metric.field().isBlank()) errors.add(metric.type() + " requires a field");
        }
    }

    private void validateTimeRange(IqlQuery.TimeRange range, List<String> errors, List<String> warnings) {
        if (range == null) { warnings.add("No time range supplied"); return; }
        if (!SocEventSchema.TIMESTAMP.equals(range.field())) errors.add("Time range field must be timestamp");
        if (range.from() == null || range.to() == null) return;
        try {
            if (!range.from().startsWith("now") && !range.to().startsWith("now")
                    && !Instant.parse(range.from()).isBefore(Instant.parse(range.to()))) errors.add("Time range from must be before to");
        } catch (RuntimeException ignored) { errors.add("Time range must use ISO-8601 or now-relative values"); }
    }

    private void allowed(String field, String usage, List<String> errors) {
        if (field == null || !SocEventSchema.FIELD_WHITELIST.contains(field)) errors.add("Unknown " + usage + " field: " + field);
    }
}
