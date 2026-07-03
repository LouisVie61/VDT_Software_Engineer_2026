package vdt.se.demo.adapter.out.elasticsearch.dsl;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import vdt.se.demo.application.dto.SearchRequest;
import vdt.se.demo.application.port.outboundPort.execution.SearchDslBuilderPort;
import vdt.se.demo.domain.model.CanonicalQueryPlan;
import vdt.se.demo.domain.model.SearchIntent;
import vdt.se.demo.domain.model.SocEventSchema;
import vdt.se.demo.domain.model.TemplateSelection;
import vdt.se.demo.domain.valueObjects.TemplateType;

import java.util.Map;
import java.util.Locale;

@Component
public class ElasticsearchSearchDslBuilder implements SearchDslBuilderPort {
    private final ObjectMapper objectMapper;

    public ElasticsearchSearchDslBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public JsonNode build(SearchRequest request, CanonicalQueryPlan plan) {
        SearchIntent intent = plan.mergedIntent();
        TemplateSelection selection = plan.templateSelection();
        ObjectNode root = objectMapper.createObjectNode();
        root.put("timeout", "5s");
        root.set("query", boolQuery(request, intent));
        int pageSize = Math.max(1, Math.min(request.getPageSize(), 500));
        if (selection.type() == TemplateType.TIME_AGGREGATION) {
            addHitsPage(root, request, pageSize);
            ObjectNode histogram = root.putObject("aggs").putObject("events_over_time").putObject("date_histogram");
            histogram.put("field", "timestamp");
            addHistogramInterval(histogram, intent);
            return root;
        }
        if (selection.type() == TemplateType.TERMS_AGGREGATION) {
            addHitsPage(root, request, pageSize);
            String aggregationName = intent != null && intent.getTopN() != null ? "top_values" : "grouped_values";
            ObjectNode terms = root.putObject("aggs").putObject(aggregationName).putObject("terms");
            terms.put("field", selection.groupBy());
            terms.put("size", selection.size());
            return root;
        }
        addHitsPage(root, request, pageSize);
        return root;
    }

    private void addHitsPage(ObjectNode root, SearchRequest request, int pageSize) {
        root.put("size", pageSize);
        root.put("track_total_hits", true);
        if (hasText(request.getSearchAfter())) {
            addSearchAfter(root, request);
        } else if (request.getPage() > 0) {
            root.put("from", Math.max(0, request.getPage()) * pageSize);
        }
        root.set("sort", timestampSort());
    }

    private void addHistogramInterval(ObjectNode histogram, SearchIntent intent) {
        String bucket = intent == null || intent.getTimeBucket() == null ? null : intent.getTimeBucket().trim().toLowerCase(Locale.ROOT);
        if ("quarter".equals(bucket) || "1q".equals(bucket)) {
            histogram.put("calendar_interval", "quarter");
            return;
        }
        if ("1w".equals(bucket) || "week".equals(bucket) || "weekly".equals(bucket)) {
            histogram.put("calendar_interval", "week");
            return;
        }
        histogram.put("fixed_interval", hasText(bucket) ? bucket : "1h");
    }

    private void addSearchAfter(ObjectNode root, SearchRequest request) {
        if (!hasText(request.getSearchAfter())) {
            return;
        }
        ArrayNode searchAfter = objectMapper.createArrayNode();
        searchAfter.add(request.getSearchAfter().trim());
        root.set("search_after", searchAfter);
    }

    private ObjectNode boolQuery(SearchRequest request, SearchIntent intent) {
        ObjectNode query = objectMapper.createObjectNode();
        ObjectNode bool = query.putObject("bool");
        ArrayNode filter = bool.putArray("filter");
        if (intent != null && intent.getFilters() != null) {
            for (Map.Entry<String, String> entry : intent.getFilters().entrySet()) {
                addTermFilter(filter, entry.getKey(), entry.getValue());
            }
        }
        addTermFilter(filter, "severity", lowercase(request.getSeverity()));
        addTermFilter(filter, "event_type", lowercase(request.getEventType()));
        addTermFilter(filter, "user", request.getUser());
        addTermFilter(filter, "host", request.getHost());
        addTermFilter(filter, "ip", request.getIp());
        addTimeRangeFilter(filter, request, intent);
        addRecurringTimeFilter(filter, intent);

        String textQuery = intent == null ? null : intent.getTextQuery();
        if (hasText(textQuery)) {
            ArrayNode must = bool.putArray("must");
            ObjectNode sqs = objectMapper.createObjectNode();
            ObjectNode body = sqs.putObject("simple_query_string");
            body.put("query", textQuery.trim());
            ArrayNode fields = body.putArray("fields");
            for (String field : SocEventSchema.FULL_TEXT_FIELDS) {
                fields.add(field);
            }
            body.put("default_operator", "and");
            must.add(sqs);
        } else if (filter.isEmpty()) {
            ArrayNode must = bool.putArray("must");
            ObjectNode matchAll = objectMapper.createObjectNode();
            matchAll.putObject("match_all");
            must.add(matchAll);
        }
        return query;
    }

    private void addTimeRangeFilter(ArrayNode filters, SearchRequest request, SearchIntent intent) {
        if (hasText(request.getFrom()) || hasText(request.getTo())) {
            addRangeFilter(filters, request.getFrom(), request.getTo(), false);
            return;
        }
        if (intent != null && (hasText(intent.getTimeFrom()) || hasText(intent.getTimeTo()))) {
            addRangeFilter(filters, intent.getTimeFrom(), intent.getTimeTo(), true);
        }
    }

    private void addRangeFilter(ArrayNode filters, String from, String to, boolean exclusiveTo) {
        ObjectNode timestamp = objectMapper.createObjectNode();
        if (hasText(from)) {
            timestamp.put("gte", from.trim());
        }
        if (hasText(to)) {
            timestamp.put(exclusiveTo ? "lt" : "lte", to.trim());
        }
        ObjectNode range = objectMapper.createObjectNode();
        range.putObject("range").set("timestamp", timestamp);
        filters.add(range);
    }

    private void addRecurringTimeFilter(ArrayNode filters, SearchIntent intent) {
        if (intent == null || intent.getRecurringTime() == null
                || intent.getRecurringTime().month() == null
                || intent.getRecurringTime().month() < 1
                || intent.getRecurringTime().month() > 12
                || !"EVERY_YEAR".equalsIgnoreCase(intent.getRecurringTime().mode())) {
            return;
        }
        ObjectNode scriptWrapper = objectMapper.createObjectNode();
        ObjectNode script = scriptWrapper.putObject("script").putObject("script");
        script.put("source", "doc['timestamp'].size()!=0 && doc['timestamp'].value.getMonthValue() == params.month");
        script.putObject("params").put("month", intent.getRecurringTime().month());
        filters.add(scriptWrapper);
    }

    private void addTermFilter(ArrayNode filters, String field, String value) {
        if (!hasText(field) || !hasText(value)) {
            return;
        }
        ObjectNode wrapper = objectMapper.createObjectNode();
        wrapper.putObject("term").put(field.trim(), value.trim());
        filters.add(wrapper);
    }

    private ArrayNode timestampSort() {
        ArrayNode sort = objectMapper.createArrayNode();
        ObjectNode timestampSort = objectMapper.createObjectNode();
        timestampSort.putObject("timestamp").put("order", "desc");
        sort.add(timestampSort);
        return sort;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String lowercase(String value) {
        return hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : value;
    }
}
