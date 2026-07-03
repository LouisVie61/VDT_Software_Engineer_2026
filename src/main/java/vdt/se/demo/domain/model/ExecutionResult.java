package vdt.se.demo.domain.model;

import lombok.Builder;

import java.util.List;
import java.util.Map;

@Builder
public record ExecutionResult(
        List<Map<String, Object>> results,
        List<Map<String, Object>> aggregations,
        int totalCount,
        List<SearchWarning> warnings
) {
    public ExecutionResult {
        results = results == null ? List.of() : results;
        aggregations = aggregations == null ? List.of() : aggregations;
        warnings = warnings == null ? List.of() : warnings;
    }

    public ExecutionResult(List<Map<String, Object>> results, List<Map<String, Object>> aggregations, int totalCount) {
        this(results, aggregations, totalCount, List.of());
    }
}
