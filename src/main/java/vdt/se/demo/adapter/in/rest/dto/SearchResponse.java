package vdt.se.demo.adapter.in.rest.dto;

import tools.jackson.databind.JsonNode;
import vdt.se.demo.domain.model.QueryResult;
import vdt.se.demo.domain.valueObjects.ChartType;

import java.util.UUID;

public record SearchResponse(
        UUID id,
        String nlQuery,
        JsonNode generatedDsl,
        String summary,
        Object results,
        Object aggregations,
        int totalCount,
        ChartType chartType,
        Integer page,
        Integer pageSize
) {
    public static SearchResponse from(QueryResult result) {
        return new SearchResponse(
                result.getId(),
                result.getNlQuery(),
                result.getGeneratedDSL(),
                result.getSummary(),
                result.getResults(),
                result.getAggregations(),
                result.getTotalCount(),
                result.getChartType(),
                result.getPage(),
                result.getPageSize()
        );
    }
}
