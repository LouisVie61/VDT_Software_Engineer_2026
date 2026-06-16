package vdt.se.demo.adapter.out.elasticsearch.dsl;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import vdt.se.demo.application.dto.SearchRequest;

import java.util.Map;

@Component
public class ElasticsearchExplicitFilterDslEditor {

    private final ObjectMapper objectMapper;
    private final ElasticsearchExplicitFilterBuilder filterBuilder;

    public ElasticsearchExplicitFilterDslEditor(ObjectMapper objectMapper,
                                                ElasticsearchExplicitFilterBuilder filterBuilder) {
        this.objectMapper = objectMapper;
        this.filterBuilder = filterBuilder;
    }

    public JsonNode withExplicitFilters(SearchRequest request, JsonNode dsl) {
        ElasticsearchExplicitFilterSet filterSet = filterBuilder.build(request);
        if (filterSet.isEmpty()) {
            return dsl;
        }

        ObjectNode root = rootObject(dsl);
        ObjectNode bool = ensureBoolQuery(root);
        ArrayNode filters = ensureFilterArray(bool);
        removeManagedFilters(filters, filterSet);
        filterSet.filters().forEach(filters::add);
        return root;
    }

    private ObjectNode rootObject(JsonNode dsl) {
        JsonNode copy = dsl.deepCopy();
        if (copy instanceof ObjectNode objectNode) {
            return objectNode;
        }

        ObjectNode root = objectMapper.createObjectNode();
        root.set("query", copy);
        return root;
    }

    private ObjectNode ensureBoolQuery(ObjectNode root) {
        JsonNode query = root.get("query");
        JsonNode bool = query == null || !query.isObject() ? null : query.get("bool");
        if (bool instanceof ObjectNode objectNode) {
            return objectNode;
        }

        ObjectNode boolQuery = objectMapper.createObjectNode();
        boolQuery.putArray("must").add(query == null || query.isNull() ? matchAll() : query.deepCopy());

        ObjectNode queryNode = objectMapper.createObjectNode();
        queryNode.set("bool", boolQuery);
        root.set("query", queryNode);
        return boolQuery;
    }

    private ArrayNode ensureFilterArray(ObjectNode bool) {
        JsonNode existing = bool.get("filter");
        if (existing instanceof ArrayNode arrayNode) {
            return arrayNode;
        }

        ArrayNode filters = objectMapper.createArrayNode();
        if (existing != null && !existing.isNull()) {
            filters.add(existing.deepCopy());
        }
        bool.set("filter", filters);
        return filters;
    }

    private void removeManagedFilters(ArrayNode filters, ElasticsearchExplicitFilterSet filterSet) {
        for (int i = filters.size() - 1; i >= 0; i--) {
            if (isManagedFilter(filters.get(i), filterSet)) {
                filters.remove(i);
            }
        }
    }

    private boolean isManagedFilter(JsonNode node, ElasticsearchExplicitFilterSet filterSet) {
        if (node == null || !node.isObject()) {
            return false;
        }
        if (filterSet.managesTimestamp() && isTimestampRange(node)) {
            return true;
        }
        if (hasManagedField(node.get("term"), filterSet) || hasManagedField(node.get("terms"), filterSet)) {
            return true;
        }

        JsonNode should = node.path("bool").get("should");
        if (should != null && should.isArray()) {
            for (JsonNode item : should) {
                if (isManagedFilter(item, filterSet)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isTimestampRange(JsonNode node) {
        JsonNode range = node.get("range");
        return range != null && range.isObject() && range.get("timestamp") != null;
    }

    private boolean hasManagedField(JsonNode node, ElasticsearchExplicitFilterSet filterSet) {
        if (node == null || !node.isObject()) {
            return false;
        }
        for (Map.Entry<String, JsonNode> entry : node.properties()) {
            if (filterSet.managedFields().contains(entry.getKey())) {
                return true;
            }
        }
        return false;
    }

    private ObjectNode matchAll() {
        ObjectNode matchAll = objectMapper.createObjectNode();
        matchAll.set("match_all", objectMapper.createObjectNode());
        return matchAll;
    }
}
