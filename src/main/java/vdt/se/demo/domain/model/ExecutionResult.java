package vdt.se.demo.domain.model;

import java.util.List;
import java.util.Map;

public record ExecutionResult(
        List<Map<String, Object>> results,
        List<Map<String, Object>> aggregations,
        int totalCount
) {
}
