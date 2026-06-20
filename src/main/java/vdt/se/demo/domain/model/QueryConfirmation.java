package vdt.se.demo.domain.model;

import lombok.Builder;

import java.util.List;

@Builder
public record QueryConfirmation(
        String confirmationId,
        SearchIntent intent,
        TemplateSelection templateSelection,
        List<SearchWarning> warnings
) {
    public QueryConfirmation {
        warnings = warnings == null ? List.of() : warnings;
    }
}
