package vdt.se.demo.domain.model;

import lombok.Builder;

import java.util.List;
import java.util.Map;

@Builder
public record QueryConfirmation(
        String confirmationId,
        SearchIntent intent,
        TemplateSelection templateSelection,
        List<SearchWarning> warnings,
        Map<String, String> requestFilters
) {
    public QueryConfirmation {
        warnings = warnings == null ? List.of() : warnings;
        requestFilters = requestFilters == null ? Map.of() : Map.copyOf(requestFilters);
    }
}
