package vdt.se.demo.domain.model;

import lombok.Builder;
import vdt.se.demo.domain.valueObjects.TemplateType;

@Builder
public record RoutingHint(
        TemplateType templateType,
        double confidence,
        String reason,
        boolean semantic,
        boolean neutral,
        boolean lowConfidencePerception
) {
    public static RoutingHint neutralLowConfidence(String reason) {
        return RoutingHint.builder()
                .confidence(0.0d)
                .reason(reason)
                .neutral(true)
                .lowConfidencePerception(true)
                .build();
    }

    public boolean lowConfidence() {
        return confidence < 0.45d;
    }

    public boolean ambiguous() {
        return confidence >= 0.45d && confidence < 0.70d;
    }
}
