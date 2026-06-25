package vdt.se.demo.application.service.template;

import org.junit.jupiter.api.Test;
import vdt.se.demo.domain.model.CanonicalQueryPlan;
import vdt.se.demo.domain.model.IntentValidationResult;
import vdt.se.demo.domain.model.RoutingHint;
import vdt.se.demo.domain.model.SearchIntent;
import vdt.se.demo.domain.valueObjects.TemplateType;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateSelectionServiceTest {

    private final TemplateSelectionService service = new TemplateSelectionService();

    @Test
    void zeroTopNMeansNoExplicitTopNLimitAndUsesDefaultAggregationCap() {
        SearchIntent intent = SearchIntent.builder()
                .intent(TemplateType.TERMS_AGGREGATION)
                .groupBy("severity")
                .topN(0)
                .build();

        IntentValidationResult result = service.validate(intent, TemplateType.TERMS_AGGREGATION);

        assertThat(result.valid()).isTrue();
        assertThat(result.selection().size()).isEqualTo(100);
    }

    @Test
    void zeroTopNUsesDefaultAggregationCapWhenConfirmationIsRequired() {
        CanonicalPlanBuilder builder = new CanonicalPlanBuilder(
                service,
                new TemplateIntentSelector(),
                new GroupByResolver());
        SearchIntent intent = SearchIntent.builder()
                .intent(TemplateType.TERMS_AGGREGATION)
                .topN(0)
                .build();

        CanonicalQueryPlan plan = builder.build(
                "top events",
                "v4",
                "session-1",
                RoutingHint.neutralLowConfidence("test"),
                intent,
                intent,
                "TEST",
                null);

        assertThat(plan.templateSelection().size()).isEqualTo(100);
    }
}
