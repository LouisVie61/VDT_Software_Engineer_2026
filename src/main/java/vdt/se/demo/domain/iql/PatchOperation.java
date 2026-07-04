package vdt.se.demo.domain.iql;

import tools.jackson.databind.JsonNode;

public record PatchOperation(Type op, String filterId, JsonNode value) {
    public enum Type { ADD_FILTER, REMOVE_FILTER, REPLACE_FILTER, SET_GROUP_BY, CLEAR_GROUP_BY,
        SET_TIME_RANGE, SET_METRICS, SET_SORT, SET_SIZE }
}
