package vdt.se.demo.adapter.in.rest.dto;

import tools.jackson.databind.JsonNode;
import vdt.se.demo.domain.model.QueryResult;
import vdt.se.demo.domain.model.SearchWarning;
import vdt.se.demo.domain.valueObjects.ChartType;
import vdt.se.demo.domain.valueObjects.SummaryStatus;

import java.util.List;
import java.util.Map;
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
        Integer pageSize,
        List<SearchWarning> warnings,
        String selectedTemplate,
        boolean cacheHit,
        SummaryStatus summaryStatus,
        Object diagnostic,
        Map<String, Double> confidenceScores,
        String overrideIntent,
        String overrideReason,
        UUID canonicalPlanId
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
                result.getPageSize(),
                result.getWarnings(),
                result.getSelectedTemplate(),
                result.isCacheHit(),
                result.getSummaryStatus(),
                result.getDiagnostic(),
                result.getConfidenceScores(),
                result.getOverrideIntent(),
                result.getOverrideReason(),
                result.getCanonicalPlanId()
        );
    }
}
