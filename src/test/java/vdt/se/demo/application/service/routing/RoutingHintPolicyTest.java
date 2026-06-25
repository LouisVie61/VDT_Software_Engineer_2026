package vdt.se.demo.application.service.routing;

import org.junit.jupiter.api.Test;
import vdt.se.demo.domain.model.RoutingHint;
import vdt.se.demo.domain.model.SearchIntent;
import vdt.se.demo.domain.valueObjects.TemplateType;

import static org.assertj.core.api.Assertions.assertThat;

class RoutingHintPolicyTest {
    private final RoutingHintPolicy policy = new RoutingHintPolicy();

    @Test
    void returnsNeutralHintForLowSignalColdStart() {
        RoutingHint hint = policy.hintForLlm(decision(lowHint(), lowHint()), null);

        assertThat(hint.neutral()).isTrue();
        assertThat(hint.lowConfidencePerception()).isTrue();
        assertThat(hint.templateType()).isNull();
    }

    @Test
    void usesPreviousIntentOnlyWhenBothSignalsAreLowConfidence() {
        SearchIntent previous = SearchIntent.builder()
                .intent(TemplateType.TIME_AGGREGATION)
                .build();

        RoutingHint hint = policy.hintForLlm(decision(lowHint(), lowHint()), previous);

        assertThat(hint.templateType()).isEqualTo(TemplateType.TIME_AGGREGATION);
        assertThat(hint.neutral()).isFalse();
    }

    @Test
    void returnsStrongestSignalWhenEvidenceExists() {
        RoutingHint hint = policy.hintForLlm(decision(lowHint(), highTermsHint()), null);

        assertThat(hint.templateType()).isEqualTo(TemplateType.TERMS_AGGREGATION);
    }

    private QueryRoutingService.RoutingDecision decision(RoutingHint heuristic, RoutingHint semantic) {
        return new QueryRoutingService.RoutingDecision(heuristic, semantic);
    }

    private RoutingHint lowHint() {
        return RoutingHint.builder()
                .templateType(TemplateType.SIMPLE_SEARCH)
                .confidence(0.30d)
                .build();
    }

    private RoutingHint highTermsHint() {
        return RoutingHint.builder()
                .templateType(TemplateType.TERMS_AGGREGATION)
                .confidence(0.72d)
                .build();
    }
}
