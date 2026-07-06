package vdt.se.demo.application.service.query;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import vdt.se.demo.domain.exception.BadQueryException;
import vdt.se.demo.domain.iql.IqlQuery;
import vdt.se.demo.domain.iql.SearchConstraints;
import vdt.se.demo.domain.model.SocEventSchema;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class IqlQueryNormalizer {
    private record EventAlias(String eventType, String action) {}
    private static final Map<String, EventAlias> COMPOSITE_EVENT_ALIASES = Map.of(
            "auth_failed", new EventAlias("auth", "failed"),
            "failed_auth", new EventAlias("auth", "failed"),
            "login_failed", new EventAlias("auth", "failed"),
            "failed_login", new EventAlias("auth", "failed"));
    private static final Map<String, int[]> TEMPORAL_COMPONENT_BOUNDS = Map.of(
            SocEventSchema.TIMESTAMP_YEAR, new int[]{0, 9999},
            SocEventSchema.TIMESTAMP_QUARTER, new int[]{1, 4},
            SocEventSchema.TIMESTAMP_MONTH, new int[]{1, 12},
            SocEventSchema.TIMESTAMP_DAY, new int[]{1, 31},
            SocEventSchema.TIMESTAMP_HOUR, new int[]{0, 23},
            SocEventSchema.TIMESTAMP_MINUTE, new int[]{0, 59},
            SocEventSchema.TIMESTAMP_SECOND, new int[]{0, 59});
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
            } else filters.add(normalizeFilterValue(filter));
        }
        filters = expandCompositeEventAliases(filters);
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
                .map(window -> new IqlQuery.Window(window.name(), normalizeWindowRange(window.timeRange(), now),
                        expandCompositeEventAliases(window.filters().stream().map(this::normalizeFilterValue).toList())))
                .toList();
    }

    private List<IqlQuery.FilterCondition> expandCompositeEventAliases(List<IqlQuery.FilterCondition> input) {
        List<IqlQuery.FilterCondition> result = new ArrayList<>(input);
        for (int i = 0; i < result.size(); i++) {
            IqlQuery.FilterCondition filter = result.get(i);
            if (!SocEventSchema.EVENT_TYPE.equals(filter.field()) || filter.op() != IqlQuery.Operator.EQ
                    || filter.value() == null || !filter.value().isString()) continue;
            EventAlias alias = COMPOSITE_EVENT_ALIASES.get(filter.value().asString().toLowerCase(java.util.Locale.ROOT));
            if (alias == null) continue;
            result.set(i, new IqlQuery.FilterCondition(filter.id(), SocEventSchema.EVENT_TYPE,
                    IqlQuery.Operator.EQ, mapper.valueToTree(alias.eventType())));
            IqlQuery.FilterCondition action = result.stream()
                    .filter(candidate -> SocEventSchema.ACTION.equals(candidate.field()))
                    .findFirst().orElse(null);
            if (action != null) {
                if (action.op() != IqlQuery.Operator.EQ || action.value() == null
                        || !alias.action().equalsIgnoreCase(action.value().asString())) {
                    throw new BadQueryException("Composite event alias conflicts with action filter: " + filter.value());
                }
            } else {
                result.add(new IqlQuery.FilterCondition(uniqueFilterId(result, filter.id() + "-action"),
                        SocEventSchema.ACTION, IqlQuery.Operator.EQ, mapper.valueToTree(alias.action())));
            }
        }
        return result;
    }

    private String uniqueFilterId(List<IqlQuery.FilterCondition> filters, String candidate) {
        String id = candidate;
        int suffix = 2;
        while (containsFilterId(filters, id)) id = candidate + "-" + suffix++;
        return id;
    }

    private boolean containsFilterId(List<IqlQuery.FilterCondition> filters, String id) {
        return filters.stream().anyMatch(filter -> id.equals(filter.id()));
    }

    private IqlQuery.FilterCondition normalizeFilterValue(IqlQuery.FilterCondition filter) {
        int[] bounds = TEMPORAL_COMPONENT_BOUNDS.get(filter.field());
        if (bounds == null || filter.op() == IqlQuery.Operator.EXISTS || filter.value() == null) return filter;
        JsonNode value = filter.value();
        JsonNode normalized;
        if (filter.op() == IqlQuery.Operator.IN || filter.op() == IqlQuery.Operator.NOT_IN) {
            if (!value.isArray()) throw new BadQueryException(filter.field() + " requires an array for " + filter.op());
            ArrayNode values = mapper.createArrayNode();
            value.forEach(item -> values.add(temporalComponent(filter.field(), item, bounds)));
            normalized = values;
        } else {
            normalized = mapper.valueToTree(temporalComponent(filter.field(), value, bounds));
        }
        return new IqlQuery.FilterCondition(filter.id(), filter.field(), filter.op(), normalized);
    }

    private int temporalComponent(String field, JsonNode value, int[] bounds) {
        Integer component = null;
        if (value.isIntegralNumber()) {
            component = value.canConvertToInt() ? value.asInt() : null;
        } else if (value.isString()) {
            String text = value.asString().trim();
            try {
                component = Integer.valueOf(text);
            } catch (NumberFormatException ignored) {
                component = componentFromIsoDate(field, text);
            }
        }
        if (component == null || component < bounds[0] || component > bounds[1]) {
            throw new BadQueryException("Invalid numeric value for " + field + ": " + value);
        }
        return component;
    }

    private Integer componentFromIsoDate(String field, String value) {
        if (!value.matches("\\d{4}-\\d{2}-\\d{2}(?:[Tt ].*)?")) return null;
        int year = Integer.parseInt(value.substring(0, 4));
        int month = Integer.parseInt(value.substring(5, 7));
        int day = Integer.parseInt(value.substring(8, 10));
        try {
            java.time.LocalDate.of(year, month, day);
        } catch (java.time.DateTimeException invalidDate) {
            return null;
        }
        return switch (field) {
            case SocEventSchema.TIMESTAMP_YEAR -> year;
            case SocEventSchema.TIMESTAMP_QUARTER -> ((month - 1) / 3) + 1;
            case SocEventSchema.TIMESTAMP_MONTH -> month;
            case SocEventSchema.TIMESTAMP_DAY -> day;
            default -> null;
        };
    }

    private IqlQuery.TimeRange normalizeWindowRange(IqlQuery.TimeRange range, Instant now) {
        if (range == null) throw new BadQueryException("Window time_range is required");
        String from = range.from() == null ? null : absolute(range.from(), now);
        String to = range.to() == null ? null : exclusiveUpperBound(absolute(range.to(), now));
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
        if (to != null) to = exclusiveUpperBound(absolute(to, now));
        return new IqlQuery.TimeRange(SocEventSchema.TIMESTAMP, from, to);
    }

    private String exclusiveUpperBound(String value) {
        if (value == null || !value.matches(".*T23:59:59Z")) return value;
        try {
            return Instant.parse(value).plusSeconds(1).toString();
        } catch (DateTimeParseException ignored) {
            return value;
        }
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
