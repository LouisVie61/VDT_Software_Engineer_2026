package vdt.se.demo.application.service.query;

import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.domain.exception.BadQueryException;
import vdt.se.demo.domain.iql.IqlQuery;
import vdt.se.demo.domain.iql.SearchConstraints;
import vdt.se.demo.domain.model.SocEventSchema;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class IqlQueryNormalizer {
    private final ObjectMapper mapper;
    private final Clock clock;

    public IqlQueryNormalizer(ObjectMapper mapper) { this(mapper, Clock.systemUTC()); }
    IqlQueryNormalizer(ObjectMapper mapper, Clock clock) { this.mapper = mapper; this.clock = clock; }

    public IqlQuery normalize(IqlQuery query, SearchConstraints constraints) {
        if (query == null) throw new BadQueryException("Query is required");
        SearchConstraints c = constraints == null ? SearchConstraints.empty() : constraints;
        List<IqlQuery.FilterCondition> filters = new ArrayList<>();
        IqlQuery.TimeRange migrated = null;
        for (IqlQuery.FilterCondition filter : query.filters()) {
            if (SocEventSchema.TIMESTAMP.equals(filter.field())) {
                migrated = migrateTimestamp(migrated, filter);
            } else filters.add(filter);
        }
        putOverride(filters, "request-severity", "severity", c.severity());
        putOverride(filters, "request-event-type", "event_type", c.eventType());
        putOverride(filters, "request-user", "user", c.user());
        putOverride(filters, "request-host", "host", c.host());
        putOverride(filters, "request-ip", "ip", c.ip());

        IqlQuery.TimeRange range = resolveRange(c, query.timeRange() != null ? query.timeRange() : migrated);
        return new IqlQuery(query.select(), filters, query.filterLogic(), range, query.groupBy(), query.metrics(),
                query.orderBy(), query.sort(), query.size(), query.pageAfter(), normalizeWindows(query.windows()),
                query.having(), query.derivedMetrics());
    }

    private List<IqlQuery.Window> normalizeWindows(List<IqlQuery.Window> windows) {
        if (windows == null || windows.isEmpty()) return List.of();
        Instant now = clock.instant();
        return windows.stream()
                .map(window -> new IqlQuery.Window(window.name(), normalizeWindowRange(window.timeRange(), now), window.filters()))
                .toList();
    }

    private IqlQuery.TimeRange normalizeWindowRange(IqlQuery.TimeRange range, Instant now) {
        if (range == null) throw new BadQueryException("Window time_range is required");
        String from = range.from() == null ? null : absolute(range.from(), now);
        String to = range.to() == null ? null : absolute(range.to(), now);
        return new IqlQuery.TimeRange(range.field(), from, to);
    }

    private void putOverride(List<IqlQuery.FilterCondition> filters, String id, String field, String value) {
        if (value == null || value.isBlank()) return;
        filters.removeIf(filter -> field.equals(filter.field()));
        filters.add(new IqlQuery.FilterCondition(id, field, IqlQuery.Operator.EQ, mapper.valueToTree(value)));
    }

    private IqlQuery.TimeRange migrateTimestamp(IqlQuery.TimeRange range, IqlQuery.FilterCondition filter) {
        String value = filter.value() != null && filter.value().isString() ? filter.value().asString() : null;
        if (value == null) throw new BadQueryException("Timestamp filter requires a string value");
        String from = range == null ? null : range.from();
        String to = range == null ? null : range.to();
        switch (filter.op()) {
            case GT, GTE -> from = value;
            case LT, LTE -> to = value;
            default -> throw new BadQueryException("Timestamp filter must use gt/gte/lt/lte");
        }
        return new IqlQuery.TimeRange(SocEventSchema.TIMESTAMP, from, to);
    }

    private IqlQuery.TimeRange resolveRange(SearchConstraints c, IqlQuery.TimeRange inferred) {
        String from = nonBlank(c.from(), inferred == null ? null : inferred.from());
        String to = nonBlank(c.to(), inferred == null ? null : inferred.to());
        if (from == null && to == null) return null;
        Instant now = clock.instant();
        if (from != null) from = absolute(from, now);
        if (to != null) to = absolute(to, now);
        return new IqlQuery.TimeRange(SocEventSchema.TIMESTAMP, from, to);
    }

    private String absolute(String value, Instant now) {
        if ("now".equals(value)) return now.toString();
        if (!value.startsWith("now-")) return value;
        String amount = value.substring(4);
        if (!amount.matches("\\d+[mhd]")) return value;
        if (amount.length() < 2) throw new BadQueryException("Invalid relative time: " + value);
        long number;
        try { number = Long.parseLong(amount.substring(0, amount.length() - 1)); }
        catch (NumberFormatException e) { throw new BadQueryException("Invalid relative time: " + value); }
        Duration duration = switch (amount.charAt(amount.length() - 1)) {
            case 'm' -> Duration.ofMinutes(number);
            case 'h' -> Duration.ofHours(number);
            case 'd' -> Duration.ofDays(number);
            default -> throw new BadQueryException("Unsupported relative time unit: " + value);
        };
        return now.minus(duration).toString();
    }

    private String nonBlank(String preferred, String fallback) {
        return preferred == null || preferred.isBlank() ? fallback : preferred;
    }
}
