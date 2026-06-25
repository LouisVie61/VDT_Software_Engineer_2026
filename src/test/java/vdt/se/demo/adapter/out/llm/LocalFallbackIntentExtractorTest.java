package vdt.se.demo.adapter.out.llm;

import org.junit.jupiter.api.Test;
import vdt.se.demo.application.dto.SearchRequest;
import vdt.se.demo.domain.model.SearchIntent;
import vdt.se.demo.domain.valueObjects.TemplateType;

import static org.assertj.core.api.Assertions.assertThat;

class LocalFallbackIntentExtractorTest {

    private final LocalFallbackIntentExtractor extractor = new LocalFallbackIntentExtractor();

    @Test
    void treatsQuarterStatisticsAsTimeAggregation() {
        SearchIntent intent = extractor.extract(SearchRequest.builder()
                .question("Thong ke loi theo quy cua nam 2023")
                .build(), null);

        assertThat(intent.getIntent()).isEqualTo(TemplateType.TIME_AGGREGATION);
        assertThat(intent.getTimeBucket()).isEqualTo("quarter");
        assertThat(intent.getTimeFrom()).isEqualTo("2023-01-01T00:00:00Z");
        assertThat(intent.getTimeTo()).isEqualTo("2024-01-01T00:00:00Z");
        assertThat(intent.getGroupBy()).isNull();
        assertThat(intent.getTextQuery()).isNull();
    }
}
