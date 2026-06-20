package vdt.se.demo.application.service.cache;

import org.junit.jupiter.api.Test;
import vdt.se.demo.application.dto.SearchRequest;
import vdt.se.demo.domain.model.CanonicalQueryPlan;
import vdt.se.demo.domain.model.SearchIntent;
import vdt.se.demo.domain.model.TemplateSelection;
import vdt.se.demo.domain.valueObjects.ChartType;
import vdt.se.demo.domain.valueObjects.TemplateType;

import static org.assertj.core.api.Assertions.assertThat;

class QueryHashServiceTest {

    private final QueryHashService service = new QueryHashService();

    @Test
    void requestHashIncludesExplicitFiltersAndPagination() {
        SearchRequest base = SearchRequest.builder()
                .question("show events")
                .page(0)
                .pageSize(50)
                .build();
        SearchRequest filtered = SearchRequest.builder()
                .question("show events")
                .page(0)
                .pageSize(50)
                .user("alice")
                .build();
        SearchRequest nextPage = SearchRequest.builder()
                .question("show events")
                .page(1)
                .pageSize(50)
                .build();

        assertThat(service.hash(base))
                .isNotEqualTo(service.hash(filtered))
                .isNotEqualTo(service.hash(nextPage));
    }

    @Test
    void requestHashIncludesSessionId() {
        SearchRequest firstSession = SearchRequest.builder()
                .question("show events")
                .sessionId("session-a")
                .build();
        SearchRequest secondSession = SearchRequest.builder()
                .question("show events")
                .sessionId("session-b")
                .build();

        assertThat(service.hash(firstSession)).isNotEqualTo(service.hash(secondSession));
    }

    @Test
    void planHashIncludesResolvedPlanContext() {
        SearchRequest request = SearchRequest.builder()
                .question("statistic cac loi trong nam 2026")
                .sessionId("session-a")
                .build();

        assertThat(service.hash(request, plan("event_type", "session-a")))
                .isNotEqualTo(service.hash(request, plan("severity", "session-a")))
                .isNotEqualTo(service.hash(request, plan("event_type", "session-b")));
    }

    private CanonicalQueryPlan plan(String groupBy, String sessionId) {
        SearchIntent intent = SearchIntent.builder()
                .intent(TemplateType.TERMS_AGGREGATION)
                .groupBy(groupBy)
                .build();
        return CanonicalQueryPlan.builder()
                .normalizedQuery("statistic cac loi trong nam 2026")
                .schemaVersion("v1")
                .sessionId(sessionId)
                .mergedIntent(intent)
                .templateSelection(TemplateSelection.builder()
                        .type(TemplateType.TERMS_AGGREGATION)
                        .groupBy(groupBy)
                        .size(10)
                        .chartHint(ChartType.BAR_CHART)
                        .build())
                .build();
    }
}
