package vdt.se.demo.application.service.template;

import org.junit.jupiter.api.Test;
import vdt.se.demo.domain.model.RoutingHint;
import vdt.se.demo.domain.model.SearchIntent;
import vdt.se.demo.domain.valueObjects.TemplateType;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateIntentSelectorTest {

    private final TemplateIntentSelector selector = new TemplateIntentSelector();

    @Test
    void defaultTopNDoesNotTurnSimpleSearchIntoAggregation() {
        SearchIntent intent = SearchIntent.builder()
                .intent(TemplateType.SIMPLE_SEARCH)
                .topN(10)
                .build();
        RoutingHint routingHint = RoutingHint.builder()
                .templateType(TemplateType.SIMPLE_SEARCH)
                .confidence(0.95d)
                .build();

        assertThat(selector.select(routingHint, intent)).isEqualTo(TemplateType.SIMPLE_SEARCH);
    }
}
