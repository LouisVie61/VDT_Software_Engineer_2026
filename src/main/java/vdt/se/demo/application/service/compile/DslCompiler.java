package vdt.se.demo.application.service.compile;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import vdt.se.demo.domain.exception.BadQueryException;
import vdt.se.demo.domain.iql.IqlQuery;
import vdt.se.demo.domain.model.SocEventSchema;

import java.util.Map;
import java.util.Set;

public final class DslCompiler {
    private static final Map<String, String> CALENDAR_INTERVAL_BY_GROUP = Map.of(
            SocEventSchema.TIMESTAMP_YEAR, "year",
            SocEventSchema.TIMESTAMP_QUARTER, "quarter",
            SocEventSchema.TIMESTAMP_MONTH, "month",
            SocEventSchema.TIMESTAMP_DAY, "day",
            SocEventSchema.TIMESTAMP_HOUR, "hour",
            SocEventSchema.TIMESTAMP_MINUTE, "minute",
            SocEventSchema.TIMESTAMP_SECOND, "second"
    );
    private final ObjectMapper mapper;
    private static final Set<String> SAFE_METRICS = Set.of("count");

    public DslCompiler(ObjectMapper mapper) { this.mapper = mapper; }

    public ObjectNode compile(IqlQuery query) {
        ObjectNode root = mapper.createObjectNode();
        root.put("track_total_hits", true);
        root.put("size", query.groupBy().isEmpty() ? query.size() : 0);
        if (!query.select().isEmpty()) root.set("_source", mapper.valueToTree(query.select()));
        addQuery(root, query);
        if (query.groupBy().isEmpty()) {
            addTopLevelAggregations(root, query);
            root.set("sort", hitSort(query));
        } else addGroupedAggregations(root, query);
        return root;
    }

    private void addQuery(ObjectNode root, IqlQuery query) {
        ArrayNode filter = mapper.createArrayNode();
        ArrayNode mustNot = mapper.createArrayNode();
        for (IqlQuery.FilterCondition condition : query.filters()) {
            JsonNode clause = clause(condition);
            if (condition.op() == IqlQuery.Operator.NEQ || condition.op() == IqlQuery.Operator.NOT_IN) mustNot.add(clause);
            else filter.add(clause);
        }

        if (query.timeRange() != null) {
            ObjectNode bounds = mapper.createObjectNode();

            if (query.timeRange().from() != null && !query.timeRange().from().isBlank()) {
                bounds.put("gte", query.timeRange().from());
            }

            if (query.timeRange().to() != null && !query.timeRange().to().isBlank()) {
                bounds.put("lt", query.timeRange().to());
            }

            if (!bounds.isEmpty()) {
                ObjectNode range = mapper.createObjectNode();
                range.set(query.timeRange().field(), bounds);

                ObjectNode wrapper = mapper.createObjectNode();
                wrapper.set("range", range);

                filter.add(wrapper);
            }
        }

        if (!filter.isEmpty() || !mustNot.isEmpty()) {
            ObjectNode bool = root.putObject("query").putObject("bool");
            if (!filter.isEmpty()) bool.set("filter", filter);
            if (!mustNot.isEmpty()) bool.set("must_not", mustNot);
        }
    }

    private JsonNode clause(IqlQuery.FilterCondition condition) {
        return switch (condition.op()) {
            case EQ, NEQ -> fieldClause("term", condition.field(), normalizeValue(condition.value()));

            case IN, NOT_IN -> termsClause(condition.field(), condition.value());

            case CONTAINS -> fieldClause("match_phrase", condition.field(), normalizeValue(condition.value()));

            case EXISTS -> {
                ObjectNode body = mapper.createObjectNode();
                body.put("field", condition.field());

                ObjectNode result = mapper.createObjectNode();
                result.set("exists", body);

                yield result;
            }

            case GT, GTE, LT, LTE -> {
                ObjectNode comparison = mapper.createObjectNode();
                comparison.set(condition.op().name().toLowerCase(), normalizeValue(condition.value()));

                yield fieldClause("range", condition.field(), comparison);
            }
        };
    }

    private ObjectNode fieldClause(String type, String field, JsonNode value) {
        ObjectNode body = mapper.createObjectNode();
        body.set(field, value);

        ObjectNode result = mapper.createObjectNode();
        result.set(type, body);

        return result;
    }

    private ObjectNode termsClause(String field, JsonNode rawValue) {
        JsonNode value = normalizeValue(rawValue);

        ArrayNode values = mapper.createArrayNode();

        if (value.isArray()) {
            value.forEach(values::add);
        } else {
            values.add(value);
        }

        ObjectNode body = mapper.createObjectNode();
        body.set(field, values);

        ObjectNode result = mapper.createObjectNode();
        result.set("terms", body);

        return result;
    }

    private void addGroupedAggregations(ObjectNode root, IqlQuery query) {
        ObjectNode parent = root.putObject("aggs");
        boolean nested = usesNestedBuckets(query);
        boolean composite = !nested && (query.groupBy().size() > 1 || query.pageAfter() != null);
        ObjectNode bucket = parent.putObject("events");
        if (nested) {
            addNestedBucket(bucket, query, 0);
        } else if (composite) {
            ObjectNode definition = bucket.putObject("composite");
            definition.put("size", query.groupBy().getFirst().effectiveSize());
            ArrayNode sources = definition.putArray("sources");
            query.groupBy().forEach(group -> sources.addObject().putObject(group.field()).putObject("terms").put("field", group.field()));
            if (query.pageAfter() != null) definition.set("after", mapper.valueToTree(query.pageAfter()));
        } else {
            IqlQuery.GroupBy group = query.groupBy().getFirst();
            String calendarInterval = CALENDAR_INTERVAL_BY_GROUP.get(group.field());
            if (calendarInterval != null) {
                ObjectNode histogram = bucket.putObject("date_histogram");
                histogram.put("field", SocEventSchema.TIMESTAMP);
                histogram.put("calendar_interval", calendarInterval);
                if (query.orderBy() != null) {
                    histogram.putObject("order").put(
                            query.orderBy().target() == IqlQuery.OrderTarget.KEY ? "_key" : "_count",
                            direction(query.orderBy().direction()));
                }
            } else {
                ObjectNode terms = bucket.putObject("terms");
                terms.put("field", group.field());
                terms.put("size", group.effectiveSize());
                addTermsOrder(terms, query);
            }
        }
        ObjectNode subAggs = nested ? deepestNestedAggs(bucket, query.groupBy().size()) : bucket.putObject("aggs");
        addWindows(subAggs, query);
        addMetrics(subAggs, query);
        addDerivedMetrics(subAggs, query);
        addHaving(subAggs, query);
        query.groupBy().stream().map(IqlQuery.GroupBy::sampleHits).filter(java.util.Objects::nonNull)
                .findFirst().ifPresent(sample -> addSampleHits(subAggs, sample));
        if (composite && query.orderBy() != null && query.orderBy().target() == IqlQuery.OrderTarget.METRIC) {
            ObjectNode sort = subAggs.putObject("ordered_buckets").putObject("bucket_sort");
            String metric = metricName(query.orderBy().metricIndex() == null ? 0 : query.orderBy().metricIndex());
            sort.putArray("sort").addObject().putObject(metric).put("order", direction(query.orderBy().direction()));
        }
    }

    private boolean usesNestedBuckets(IqlQuery query) {
        return query.pageAfter() == null
                && query.groupBy().size() > 1
                && query.orderBy() != null
                && query.orderBy().target() == IqlQuery.OrderTarget.COUNT;
    }

    private void addNestedBucket(ObjectNode bucket, IqlQuery query, int index) {
        IqlQuery.GroupBy group = query.groupBy().get(index);
        String calendarInterval = CALENDAR_INTERVAL_BY_GROUP.get(group.field());
        if (calendarInterval != null) {
            ObjectNode histogram = bucket.putObject("date_histogram");
            histogram.put("field", SocEventSchema.TIMESTAMP);
            histogram.put("calendar_interval", calendarInterval);
            histogram.putObject("order").put("_count", direction(query.orderBy().direction()));
        } else {
            ObjectNode terms = bucket.putObject("terms");
            terms.put("field", group.field());
            terms.put("size", group.effectiveSize());
            terms.putObject("order").put("_count", direction(query.orderBy().direction()));
        }

        ObjectNode aggs = bucket.putObject("aggs");
        if (calendarInterval != null) {
            ObjectNode sort = aggs.putObject("limit_" + group.field()).putObject("bucket_sort");
            sort.putArray("sort").addObject().putObject("_count").put("order", direction(query.orderBy().direction()));
            sort.put("size", group.effectiveSize());
        }
        if (index + 1 < query.groupBy().size()) {
            addNestedBucket(aggs.putObject(query.groupBy().get(index + 1).field()), query, index + 1);
        }
    }

    private ObjectNode deepestNestedAggs(ObjectNode bucket, int groupCount) {
        ObjectNode current = bucket;
        for (int i = 1; i < groupCount; i++) {
            ObjectNode aggs = (ObjectNode) current.get("aggs");
            ObjectNode next = null;
            for (Map.Entry<String, JsonNode> entry : aggs.properties()) {
                if (entry.getValue().isObject()
                        && (entry.getValue().has("terms") || entry.getValue().has("date_histogram"))) {
                    next = (ObjectNode) entry.getValue();
                    break;
                }
            }
            if (next == null) {
                throw new BadQueryException("Invalid nested aggregation structure");
            }
            current = next;
        }
        JsonNode aggs = current.get("aggs");
        if (aggs != null && aggs.isObject()) {
            return (ObjectNode) aggs;
        }
        return current.putObject("aggs");
    }

    private void addTermsOrder(ObjectNode terms, IqlQuery query) {
        if (query.orderBy() == null) {
            return;
        }

        String target = resolveTermsOrderTarget(query);

        terms.putObject("order")
                .put(target, direction(query.orderBy().direction()));
    }

    private String resolveTermsOrderTarget(IqlQuery query) {
        IqlQuery.OrderBy orderBy = query.orderBy();

        if (orderBy == null) {
            return "_count";
        }

        return switch (orderBy.target()) {
            case KEY -> "_key";
            case COUNT -> "_count";
            case METRIC -> resolveMetricOrderTarget(query, orderBy.metricIndex());
        };
    }

    private String resolveMetricOrderTarget(IqlQuery query, Integer metricIndex) {
        int index = metricIndex == null ? 0 : metricIndex;

        if (index < 0) {
            throw new BadQueryException("metric_index cannot be negative");
        }

        if (index >= query.metrics().size()) {
            throw new BadQueryException("metric_index is out of range");
        }

        IqlQuery.Metric metric = query.metrics().get(index);

        if (metric.type() == IqlQuery.MetricType.COUNT) {
            return "_count";
        }

        return metricName(index);
    }

    private void addTopLevelAggregations(ObjectNode root, IqlQuery query) {
        if (!query.metrics().isEmpty() || !query.windows().isEmpty()) {
            ObjectNode aggs = root.putObject("aggs");
            addWindows(aggs, query);
            addMetrics(aggs, query);
        }
    }

    private void addMetrics(ObjectNode aggs, IqlQuery query) {
        for (int i = 0; i < query.metrics().size(); i++) {
            IqlQuery.Metric metric = query.metrics().get(i);
            if (metric.type() == IqlQuery.MetricType.COUNT) continue;
            String type = metric.type().name().toLowerCase();
            aggs.putObject(metricName(i)).putObject(type).put("field", metric.field());
        }
    }

    private void addWindows(ObjectNode aggs, IqlQuery query) {
        for (IqlQuery.Window window : query.windows()) {
            ObjectNode filterAgg = aggs.putObject(window.name()).putObject("filter");
            ArrayNode filters = mapper.createArrayNode();
            addTimeRangeFilter(filters, window.timeRange());
            for (IqlQuery.FilterCondition condition : window.filters()) {
                JsonNode clause = clause(condition);
                if (condition.op() == IqlQuery.Operator.NEQ || condition.op() == IqlQuery.Operator.NOT_IN) {
                    ObjectNode bool = mapper.createObjectNode();
                    bool.putObject("bool").putArray("must_not").add(clause);
                    filters.add(bool);
                } else {
                    filters.add(clause);
                }
            }
            if (filters.size() == 1) {
                filterAgg.setAll((ObjectNode) filters.get(0));
            } else {
                filterAgg.putObject("bool").set("filter", filters);
            }
        }
    }

    private void addTimeRangeFilter(ArrayNode filters, IqlQuery.TimeRange range) {
        if (range == null) {
            return;
        }
        ObjectNode bounds = mapper.createObjectNode();
        if (range.from() != null && !range.from().isBlank()) bounds.put("gte", range.from());
        if (range.to() != null && !range.to().isBlank()) bounds.put("lt", range.to());
        if (bounds.isEmpty()) return;
        ObjectNode rangeNode = mapper.createObjectNode();
        rangeNode.set(range.field(), bounds);
        ObjectNode wrapper = mapper.createObjectNode();
        wrapper.set("range", rangeNode);
        filters.add(wrapper);
    }

    private void addDerivedMetrics(ObjectNode aggs, IqlQuery query) {
        for (IqlQuery.DerivedMetric derived : query.derivedMetrics()) {
            ObjectNode bucketScript = aggs.putObject(derived.name()).putObject("bucket_script");
            ObjectNode paths = bucketScript.putObject("buckets_path");
            paths.put("num", metricPath(derived.numerator()));
            paths.put("den", metricPath(derived.denominator()));
            String expression = derived.type() == IqlQuery.DerivedMetricType.PERCENT
                    ? "params.den == 0 ? null : (params.num / params.den) * 100"
                    : "params.den == 0 ? null : params.num / params.den";
            bucketScript.putObject("script").put("source", expression);
        }
    }

    private void addHaving(ObjectNode aggs, IqlQuery query) {
        if (query.having().isEmpty()) {
            return;
        }
        ObjectNode selector = aggs.putObject("having").putObject("bucket_selector");
        ObjectNode paths = selector.putObject("buckets_path");
        StringBuilder source = new StringBuilder();
        for (int i = 0; i < query.having().size(); i++) {
            IqlQuery.HavingCondition condition = query.having().get(i);
            String param = "p" + i;
            paths.put(param, metricPath(new IqlQuery.MetricRef(condition.metric(), condition.window())));
            if (i > 0) source.append(" && ");
            source.append("params.").append(param).append(' ')
                    .append(comparison(condition.op())).append(' ')
                    .append(condition.value());
        }
        selector.putObject("script").put("source", source.toString());
    }

    private String metricPath(IqlQuery.MetricRef ref) {
        if (ref == null || ref.metric() == null || !SAFE_METRICS.contains(ref.metric())) {
            throw new BadQueryException("Unsupported pipeline metric reference");
        }
        if (ref.window() == null || ref.window().isBlank()) {
            return "_count";
        }
        return ref.window() + ">_count";
    }

    private String comparison(IqlQuery.ComparisonOp op) {
        return switch (op) {
            case EQ -> "==";
            case NEQ -> "!=";
            case GT -> ">";
            case GTE -> ">=";
            case LT -> "<";
            case LTE -> "<=";
        };
    }

    private ArrayNode hitSort(IqlQuery query) {
        ArrayNode result = mapper.createArrayNode();

        for (IqlQuery.Sort sort : query.sort()) {
            if ("_id".equals(sort.field())) {
                throw new BadQueryException("Cannot sort by _id. Use timestamp or event_id instead.");
            }

            result.addObject()
                    .putObject(sort.field())
                    .put("order", direction(sort.order()));
        }

        if (result.isEmpty()) {
            result.addObject()
                    .putObject("timestamp")
                    .put("order", "desc")
                    .put("unmapped_type", "date");
        }

        if (query.sort().stream().noneMatch(sort -> SocEventSchema.EVENT_ID.equals(sort.field()))) {
            result.addObject()
                    .putObject(SocEventSchema.EVENT_ID)
                    .put("order", "asc")
                    .put("unmapped_type", "keyword");
        }

        return result;
    }

    private String metricName(int index) {
        if (index < 0) throw new BadQueryException("metric_index cannot be negative");
        return "metric_" + index;
    }

    private void addSampleHits(ObjectNode subAggs, IqlQuery.SampleHits sample) {
        ObjectNode topHits = subAggs.putObject("sample_hits").putObject("top_hits");
        topHits.put("size", sample.effectiveSize());
        if (!sample.sort().isEmpty()) {
            ArrayNode sort = topHits.putArray("sort");
            sample.sort().forEach(item -> sort.addObject().putObject(item.field())
                    .put("order", direction(item.order())));
        }
    }
    private String direction(IqlQuery.Direction direction) { return direction == IqlQuery.Direction.ASC ? "asc" : "desc"; }

    private JsonNode normalizeValue(JsonNode value) {
        if (value == null || value.isNull()) {
            return mapper.nullNode();
        }

        if (value.isString()) {
            String text = value.stringValue().trim();

            JsonNode parsed = tryParseJson(text);
            if (parsed != null) {
                return normalizeValue(parsed);
            }

            if ((text.startsWith("[") && text.endsWith("]"))
                    || (text.startsWith("{") && text.endsWith("}"))) {
                String unescaped = text
                        .replace("\\\"", "\"")
                        .replace("\\\\", "\\");

                parsed = tryParseJson(unescaped);
                if (parsed != null) {
                    return normalizeValue(parsed);
                }
            }
        }

        if (value.isArray() && value.size() == 1 && value.get(0).isString()) {
            JsonNode nested = normalizeValue(value.get(0));

            if (nested.isArray() || nested.isObject()) {
                return nested;
            }
        }

        return value;
    }

    private JsonNode tryParseJson(String text) {
        try {
            return mapper.readTree(text);
        } catch (Exception ignored) {
            return null;
        }
    }
}
