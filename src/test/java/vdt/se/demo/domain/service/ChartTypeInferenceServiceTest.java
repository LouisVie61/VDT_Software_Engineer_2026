package vdt.se.demo.domain.service;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.domain.valueObjects.ChartType;

import static org.assertj.core.api.Assertions.assertThat;

class ChartTypeInferenceServiceTest {

    private final ChartTypeInferenceService service = new ChartTypeInferenceService(new ObjectMapper());

    @Test
    void returnsTableForSearchDsl() {
        assertThat(service.inferChartType("{\"query\":{\"match_all\":{}}}")).isEqualTo(ChartType.TABLE);
    }

    @Test
    void returnsLineForDateHistogram() {
        assertThat(service.inferChartType("""
                {"aggs":{"events_over_time":{"date_histogram":{"field":"timestamp"}}}}
                """)).isEqualTo(ChartType.LINE_CHART);
    }

    @Test
    void returnsBarForSizedTermsAggregation() {
        assertThat(service.inferChartType("""
                {"aggs":{"top_values":{"terms":{"field":"ip","size":10}}}}
                """)).isEqualTo(ChartType.BAR_CHART);
    }
}
