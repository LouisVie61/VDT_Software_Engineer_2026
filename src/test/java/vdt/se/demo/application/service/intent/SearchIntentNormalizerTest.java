package vdt.se.demo.application.service.intent;

import org.junit.jupiter.api.Test;
import vdt.se.demo.application.dto.SearchRequest;
import vdt.se.demo.domain.model.SearchIntent;
import vdt.se.demo.domain.valueObjects.TemplateType;

import static org.assertj.core.api.Assertions.assertThat;

class SearchIntentNormalizerTest {

    private final SearchIntentNormalizer normalizer = new SearchIntentNormalizer();

    @Test
    void removesVietnameseQuestionWordsFromStatisticalErrorQuery() {
        SearchIntent intent = SearchIntent.builder()
                .intent(TemplateType.TERMS_AGGREGATION)
                .metric("COUNT")
                .build();
        SearchRequest request = SearchRequest.builder()
                .question("Statistic event trong nam 2024; Dau la loi nhieu nhat?")
                .build();

        SearchIntent normalized = normalizer.normalize(request, intent);

        assertThat(normalized.getTextQuery()).isNull();
        assertThat(normalized.getTimeFrom()).isEqualTo("2024-01-01T00:00:00Z");
        assertThat(normalized.getTimeTo()).isEqualTo("2025-01-01T00:00:00Z");
    }

    @Test
    void consumesResolvedSemanticSpansBeforeBuildingFreeText() {
        SearchRequest request = SearchRequest.builder()
                .question("SS event trong ngay 06/01/2025")
                .build();

        SearchIntent normalized = normalizer.normalize(request, SearchIntent.builder().build());

        assertThat(normalized.getTextQuery()).isNull();
        assertThat(normalized.getTimeFrom()).isEqualTo("2025-06-01T00:00:00Z");
        assertThat(normalized.getTimeTo()).isEqualTo("2025-06-02T00:00:00Z");
        assertThat(normalized.getFilters()).doesNotContainEntry("action", "success");
    }

    @Test
    void keepsFailedLoginAsSearchTextAndConsumesYear() {
        SearchRequest request = SearchRequest.builder()
                .question("Show Failed login in 2025")
                .build();

        SearchIntent normalized = normalizer.normalize(request, SearchIntent.builder().build());

        assertThat(normalized.getTextQuery()).isEqualTo("failed");
        assertThat(normalized.getFilters()).containsEntry("event_type", "auth");
        assertThat(normalized.getFilters()).doesNotContainEntry("action", "failed");
        assertThat(normalized.getTimeFrom()).isEqualTo("2025-01-01T00:00:00Z");
        assertThat(normalized.getTimeTo()).isEqualTo("2026-01-01T00:00:00Z");
    }

    @Test
    void consumesTimeBucketAndFieldAliasesFromFreeText() {
        SearchRequest request = SearchRequest.builder()
                .question("statistics alert theo gio source IP 24h qua")
                .build();
        SearchIntent intent = SearchIntent.builder()
                .intent(TemplateType.TIME_AGGREGATION)
                .build();

        SearchIntent normalized = normalizer.normalize(request, intent);

        assertThat(normalized.getTextQuery()).isNull();
        assertThat(normalized.getTimeBucket()).isEqualTo("1h");
        assertThat(normalized.getTimeFrom()).isNotBlank();
        assertThat(normalized.getTimeTo()).isNotBlank();
    }

    @Test
    void dropsUninformativeResidualTokensForAggregationQueries() {
        SearchRequest request = SearchRequest.builder()
                .question("Statistic cac loi trong nam 2026")
                .build();
        SearchIntent intent = SearchIntent.builder()
                .intent(TemplateType.TERMS_AGGREGATION)
                .metric("COUNT")
                .groupBy("event_type")
                .build();

        SearchIntent normalized = normalizer.normalize(request, intent);

        assertThat(normalized.getTextQuery()).isNull();
        assertThat(normalized.getTimeFrom()).isEqualTo("2026-01-01T00:00:00Z");
        assertThat(normalized.getTimeTo()).isEqualTo("2027-01-01T00:00:00Z");
    }

    @Test
    void keepsConcreteResidualTokensForAggregationQueries() {
        SearchRequest request = SearchRequest.builder()
                .question("statistics malware trong nam 2026")
                .build();
        SearchIntent intent = SearchIntent.builder()
                .intent(TemplateType.TERMS_AGGREGATION)
                .metric("COUNT")
                .groupBy("event_type")
                .build();

        SearchIntent normalized = normalizer.normalize(request, intent);

        assertThat(normalized.getTextQuery()).isEqualTo("malware");
    }
}
