package vdt.se.demo.application.service.compile;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import vdt.se.demo.domain.exception.BadQueryException;
import vdt.se.demo.domain.iql.IqlQuery;
import vdt.se.demo.domain.model.SocEventSchema;

public final class DslCompiler {
    private final ObjectMapper mapper;

    public DslCompiler(ObjectMapper mapper) { this.mapper = mapper; }

    public ObjectNode compile(IqlQuery query) {
        ObjectNode root = mapper.createObjectNode();
        root.put("track_total_hits", true);
        root.put("size", query.groupBy().isEmpty() ? query.size() : 0);
        if (!query.select().isEmpty()) root.set("_source", mapper.valueToTree(query.select()));
        addQuery(root, query);
        if (query.groupBy().isEmpty()) {
            addTopLevelMetrics(root, query);
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
            case EQ, NEQ -> fieldClause("term", condition.field(), condition.value());
            case IN, NOT_IN -> fieldClause("terms", condition.field(), condition.value());
            case CONTAINS -> fieldClause("match_phrase", condition.field(), condition.value());
            case EXISTS -> { ObjectNode body = mapper.createObjectNode(); body.put("field", condition.field()); ObjectNode result = mapper.createObjectNode(); result.set("exists", body); yield result; }
            case GT, GTE, LT, LTE -> {
                ObjectNode comparison = mapper.createObjectNode(); comparison.set(condition.op().name().toLowerCase(), condition.value());
                yield fieldClause("range", condition.field(), comparison);
            }
        };
    }

    private ObjectNode fieldClause(String type, String field, JsonNode value) {
        ObjectNode body = mapper.createObjectNode(); body.set(field, value);
        ObjectNode result = mapper.createObjectNode(); result.set(type, body); return result;
    }

    private void addGroupedAggregations(ObjectNode root, IqlQuery query) {
        ObjectNode parent = root.putObject("aggs");
        boolean composite = query.groupBy().size() > 1 || query.pageAfter() != null;
        ObjectNode bucket = parent.putObject("events");
        if (composite) {
            ObjectNode definition = bucket.putObject("composite");
            definition.put("size", query.groupBy().getFirst().effectiveSize());
            ArrayNode sources = definition.putArray("sources");
            query.groupBy().forEach(group -> sources.addObject().putObject(group.field()).putObject("terms").put("field", group.field()));
            if (query.pageAfter() != null) definition.set("after", mapper.valueToTree(query.pageAfter()));
        } else {
            IqlQuery.GroupBy group = query.groupBy().getFirst();
            ObjectNode terms = bucket.putObject("terms"); terms.put("field", group.field()); terms.put("size", group.effectiveSize());
            addTermsOrder(terms, query);
        }
        ObjectNode subAggs = bucket.putObject("aggs");
        addMetrics(subAggs, query);
        query.groupBy().stream().map(IqlQuery.GroupBy::sampleHits).filter(java.util.Objects::nonNull)
                .findFirst().ifPresent(sample -> addSampleHits(subAggs, sample));
        if (composite && query.orderBy() != null && query.orderBy().target() == IqlQuery.OrderTarget.METRIC) {
            ObjectNode sort = subAggs.putObject("ordered_buckets").putObject("bucket_sort");
            String metric = metricName(query.orderBy().metricIndex() == null ? 0 : query.orderBy().metricIndex());
            sort.putArray("sort").addObject().putObject(metric).put("order", direction(query.orderBy().direction()));
        }
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

    private void addTopLevelMetrics(ObjectNode root, IqlQuery query) {
        if (!query.metrics().isEmpty()) addMetrics(root.putObject("aggs"), query);
    }

    private void addMetrics(ObjectNode aggs, IqlQuery query) {
        for (int i = 0; i < query.metrics().size(); i++) {
            IqlQuery.Metric metric = query.metrics().get(i);
            if (metric.type() == IqlQuery.MetricType.COUNT) continue;
            String type = metric.type().name().toLowerCase();
            aggs.putObject(metricName(i)).putObject(type).put("field", metric.field());
        }
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
}
