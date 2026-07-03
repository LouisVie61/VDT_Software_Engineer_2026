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

        assertThat(decision.heuristic().templateType()).isEqualTo(TemplateType.TERMS_AGGREGATION);
        assertThat(decision.strongestSignal().templateType()).isEqualTo(TemplateType.TERMS_AGGREGATION);
    }

    @Test
    void routesStatisticalErrorYearQuestionAsTimeAggregationWithoutGroupingSignal() {
        QueryRoutingService service = new QueryRoutingService((left, right) -> 0.0d);

        QueryRoutingService.RoutingDecision decision =
                service.route("Statistic cac loi trong nam 2026");

        assertThat(decision.heuristic().templateType()).isEqualTo(TemplateType.TIME_AGGREGATION);
    }

    @Test
    void timePhraseAloneDoesNotRouteAsTimeAggregation() {
        QueryRoutingService service = new QueryRoutingService((left, right) -> 0.0d);

        QueryRoutingService.RoutingDecision decision =
                service.route("from June 1 to June 15 2025");

        assertThat(decision.heuristic().templateType()).isEqualTo(TemplateType.SIMPLE_SEARCH);
    }

    @Test
    void runsSemanticExtractorEvenWhenHeuristicIsConfident() {
        QueryRoutingService service = new QueryRoutingService((left, right) -> 0.31d);

        QueryRoutingService.RoutingDecision decision = service.route("top errors");

        assertThat(decision.semantic().semantic()).isTrue();
        assertThat(decision.semantic().confidence()).isGreaterThan(0.0d);
    }
}
