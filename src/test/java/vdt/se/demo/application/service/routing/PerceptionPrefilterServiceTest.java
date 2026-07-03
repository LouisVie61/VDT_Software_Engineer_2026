package vdt.se.demo.application.service.routing;

import org.junit.jupiter.api.Test;
import vdt.se.demo.application.dto.SearchRequest;
import vdt.se.demo.application.service.template.GroupByResolver;
import vdt.se.demo.domain.model.RoutingHint;
import vdt.se.demo.domain.model.SearchIntent;
import vdt.se.demo.domain.valueObjects.TemplateType;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PerceptionPrefilterServiceTest {

    private final PerceptionPrefilterService service = new PerceptionPrefilterService(new GroupByResolver());

    @Test
    void materializesObviousTermsAggregationWithoutLlm() {
        Optional<SearchIntent> intent = service.prefilter(
                SearchRequest.builder().question("top 10 ip").build(),
                decision(TemplateType.TERMS_AGGREGATION, 0.80d),
                false);

        assertThat(intent).isPresent();
        assertThat(intent.get().getIntent()).isEqualTo(TemplateType.TERMS_AGGREGATION);
        assertThat(intent.get().getGroupBy()).isEqualTo("ip");
        assertThat(intent.get().getTopN()).isEqualTo(10);
    }

    @Test
    void defersAmbiguousFilteredAggregationToLlm() {
        Optional<SearchIntent> intent = service.prefilter(
                SearchRequest.builder().question("top 10 ip critical this month").build(),
                decision(TemplateType.TERMS_AGGREGATION, 0.80d),
                false);

        assertThat(intent).isEmpty();
    }

    @Test
    void defersFollowUpQuestionsToLlm() {
        Optional<SearchIntent> intent = service.prefilter(
                SearchRequest.builder().question("top 10 ip").build(),
                decision(TemplateType.TERMS_AGGREGATION, 0.80d),
                true);

        assertThat(intent).isEmpty();
    }

    private QueryRoutingService.RoutingDecision decision(TemplateType type, double confidence) {
        RoutingHint hint = RoutingHint.builder()
                .templateType(type)
                .confidence(confidence)
                .reason("test")
                .build();
        return new QueryRoutingService.RoutingDecision(hint, RoutingHint.neutralLowConfidence("test"));
    }
}
