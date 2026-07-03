package vdt.se.demo.adapter.out.elasticsearch.dsl;

import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Set;

public record ElasticsearchExplicitFilterSet(
        List<JsonNode> filters,
        Set<String> managedFields,
        boolean managesTimestamp
) {

    public boolean isEmpty() {
        return filters.isEmpty();
    }
}
