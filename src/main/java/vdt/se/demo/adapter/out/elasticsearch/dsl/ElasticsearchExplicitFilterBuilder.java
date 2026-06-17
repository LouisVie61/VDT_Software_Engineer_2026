                                                                                                                        package vdt.se.demo.adapter.out.elasticsearch.dsl;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import vdt.se.demo.application.dto.SearchRequest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class ElasticsearchExplicitFilterBuilder {

    private final ObjectMapper objectMapper;

    public ElasticsearchExplicitFilterBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ElasticsearchExplicitFilterSet build(SearchRequest request) {
        List<JsonNode> filters = new ArrayList<>();
        Set<String> managedFields = new HashSet<>();

        addTermFilter(filters, managedFields, "severity", request.getSeverity());
        addTermFilter(filters, managedFields, "event_type", request.getEventType());
        addTermFilter(filters, managedFields, "user", request.getUser());
        addTermFilter(filters, managedFields, "host", request.getHost());
        addIpFilter(filters, managedFields, request.getIp());

        boolean managesTimestamp = hasText(request.getFrom()) || hasText(request.getTo());
        if (managesTimestamp) {
            filters.add(timeRangeFilter(request.getFrom(), request.getTo()));
        }

        return new ElasticsearchExplicitFilterSet(filters, managedFields, managesTimestamp);
    }

    private void addTermFilter(List<JsonNode> filters, Set<String> managedFields, String field, String value) {
        if (!hasText(value)) {
            return;
        }
        managedFields.add(field);
        filters.add(term(field, value.trim()));
    }

    private void addIpFilter(List<JsonNode> filters, Set<String> managedFields, String value) {
        if (!hasText(value)) {
            return;
        }
        managedFields.add("ip");
        filters.add(term("ip", value.trim()));
    }

    private ObjectNode timeRangeFilter(String from, String to) {
        ObjectNode range = objectMapper.createObjectNode();
        ObjectNode timestamp = range.putObject("range").putObject("timestamp");
        if (hasText(from)) {
            timestamp.put("gte", from.trim());
        }
        if (hasText(to)) {
            timestamp.put("lte", to.trim());
        }
        return range;
    }

    private ObjectNode term(String field, String value) {
        ObjectNode term = objectMapper.createObjectNode();
        term.putObject("term").put(field, value);
        return term;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
