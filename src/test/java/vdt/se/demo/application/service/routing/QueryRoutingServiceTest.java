package vdt.se.demo.application.service.routing;

import org.junit.jupiter.api.Test;
import vdt.se.demo.domain.valueObjects.TemplateType;

import static org.assertj.core.api.Assertions.assertThat;

class QueryRoutingServiceTest {

    @Test
    void routesMostCommonErrorQuestionAsTermsAggregation() {
        QueryRoutingService service = new QueryRoutingService((left, right) -> 0.0d);

        QueryRoutingService.RoutingDecision decision =
                service.route("Statistic event trong nam 2024; Dau la loi nhieu nhat?");

        assertThat(decision.effective().templateType()).isEqualTo(TemplateType.TERMS_AGGREGATION);
    }

    @Test
    void routesStatisticalErrorYearQuestionAsTermsAggregation() {
        QueryRoutingService service = new QueryRoutingService((left, right) -> 0.0d);

        QueryRoutingService.RoutingDecision decision =
                service.route("Statistic cac loi trong nam 2026");

        assertThat(decision.effective().templateType()).isEqualTo(TemplateType.TERMS_AGGREGATION);
    }
}
