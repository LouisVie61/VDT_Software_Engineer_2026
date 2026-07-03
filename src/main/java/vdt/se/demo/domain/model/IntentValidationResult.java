package vdt.se.demo.domain.model;

import lombok.Builder;

import java.util.List;

@Builder
public record IntentValidationResult(
        boolean valid,
        TemplateSelection selection,
        List<SearchWarning> warnings,
        String errorMessage
) {
    public IntentValidationResult {
        warnings = warnings == null ? List.of() : warnings;
    }

    public static IntentValidationResult valid(TemplateSelection selection, List<SearchWarning> warnings) {
        return IntentValidationResult.builder()
                .valid(true)
                .selection(selection)
                .warnings(warnings)
                .build();
    }

    public static IntentValidationResult invalid(String errorMessage) {
        return IntentValidationResult.builder()
                .valid(false)
                .errorMessage(errorMessage)
                .build();
    }
}
