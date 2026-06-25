package vdt.se.demo.application.service.intent;

import org.junit.jupiter.api.Test;
import vdt.se.demo.application.dto.SearchRequest;
import vdt.se.demo.domain.model.SearchIntent;
import vdt.se.demo.domain.model.SemanticSpan;
import vdt.se.demo.domain.valueObjects.TemplateType;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SearchIntentNormalizerTest {

    private final SearchIntentNormalizer normalizer = new SearchIntentNormalizer();

    @Test
    void keepsLlmLinguisticExtractionInsteadOfReParsingQuestion() {
        SearchRequest request = SearchRequest.builder()
                .question("Show Failed login in 2025")
                .build();
        SearchIntent llmIntent = SearchIntent.builder()
                .intent(TemplateType.SIMPLE_SEARCH)
                .textQuery("failed")
                .filters(Map.of("event_type", "auth"))
                .timeFrom("2025-01-01T00:00:00Z")
                .timeTo("2026-01-01T00:00:00Z")
                .build();

        SearchIntent normalized = normalizer.normalize(request, llmIntent);

        assertThat(normalized.getTextQuery()).isEqualTo("failed");
        assertThat(normalized.getFilters()).containsEntry("event_type", "auth");
        assertThat(normalized.getTimeFrom()).isEqualTo("2025-01-01T00:00:00Z");
        assertThat(normalized.getTimeTo()).isEqualTo("2026-01-01T00:00:00Z");
    }

    @Test
    void doesNotInferFiltersOrTextWhenLlmLeavesAmbiguousLanguageEmptyButResolvesTime() {
        SearchRequest request = SearchRequest.builder()
                .question("Show Failed login in 2025")
                .build();

        SearchIntent normalized = normalizer.normalize(request, SearchIntent.builder().build());

        assertThat(normalized.getTextQuery()).isNull();
        assertThat(normalized.getFilters()).isEmpty();
        assertThat(normalized.getTimeFrom()).isEqualTo("2025-01-01T00:00:00Z");
        assertThat(normalized.getTimeTo()).isEqualTo("2026-01-01T00:00:00Z");
    }

    @Test
    void resolvesTemporalSpanOnlyWhenLlmSuppliesOne() {
        SearchRequest request = SearchRequest.builder()
                .question("show events in 2025")
                .build();
        SearchIntent llmIntent = SearchIntent.builder()
                .semanticSpans(java.util.List.of(SemanticSpan.builder()
                        .kind(SemanticSpan.Kind.TEMPORAL)
                        .status(SemanticSpan.Status.RESOLVED)
                        .text("2025")
                        .canonical("year_2025")
                        .start(15)
                        .end(19)
                        .build()))
                .build();

        SearchIntent normalized = normalizer.normalize(request, llmIntent);

        assertThat(normalized.getTimeFrom()).isEqualTo("2025-01-01T00:00:00Z");
        assertThat(normalized.getTimeTo()).isEqualTo("2026-01-01T00:00:00Z");
    }

    @Test
    void keepsMissingTermsAggregationTopNForConfirmation() {
        SearchRequest request = SearchRequest.builder()
                .question("top 25 ip")
                .build();
        SearchIntent llmIntent = SearchIntent.builder()
                .intent(TemplateType.TERMS_AGGREGATION)
                .groupBy("ip")
                .build();

        SearchIntent normalized = normalizer.normalize(request, llmIntent);

        assertThat(normalized.getTopN()).isNull();
    }

    @Test
    void removesInventedSeverityValuesFromLlmOutput() {
        SearchRequest request = SearchRequest.builder()
                .question("show login failures")
                .build();
        SearchIntent llmIntent = SearchIntent.builder()
                .filters(Map.of("event_type", "login_failure", "severity", "urgent"))
                .build();

        SearchIntent normalized = normalizer.normalize(request, llmIntent);

        assertThat(normalized.getFilters())
                .containsEntry("event_type", "login_failure")
                .doesNotContainKey("severity");
    }

    @Test
    void keepsFreeFormFilterValuesAfterSchemaValidation() {
        SearchRequest request = SearchRequest.builder()
                .question("show alice on host-1")
                .build();
        SearchIntent llmIntent = SearchIntent.builder()
                .filters(Map.of("user", " alice ", "host", " host-1 "))
                .build();

        SearchIntent normalized = normalizer.normalize(request, llmIntent);

        assertThat(normalized.getFilters())
                .containsEntry("user", "alice")
                .containsEntry("host", "host-1");
    }

    @Test
    void removesNonFilterableFieldsFromLlmOutput() {
        SearchRequest request = SearchRequest.builder()
                .question("show message equals failed")
                .build();
        SearchIntent llmIntent = SearchIntent.builder()
                .filters(Map.of("message", "failed", "severity", "HIGH"))
                .build();

        SearchIntent normalized = normalizer.normalize(request, llmIntent);

        assertThat(normalized.getFilters())
                .containsEntry("severity", "high")
                .doesNotContainKey("message");
    }

    @Test
    void resolvesYearPhraseFromQuestionWhenLlmOmitsTemporalSpan() {
        SearchRequest request = SearchRequest.builder()
                .question("Search ip duoc nhieu nhat trong nam 2021")
                .build();

        SearchIntent normalized = normalizer.normalize(request, SearchIntent.builder()
                .intent(TemplateType.TERMS_AGGREGATION)
                .groupBy("ip")
                .build());

        assertThat(normalized.getTimeFrom()).isEqualTo("2021-01-01T00:00:00Z");
        assertThat(normalized.getTimeTo()).isEqualTo("2022-01-01T00:00:00Z");
    }

    @Test
    void resolvesVietnameseRelativeWindowFromQuestion() {
        SearchRequest request = SearchRequest.builder()
                .question("dem so alert trong 7 ngay qua")
                .build();

        SearchIntent normalized = normalizer.normalize(request, SearchIntent.builder().build());

        assertThat(normalized.getTimeFrom()).isNotNull();
        assertThat(normalized.getTimeTo()).isNotNull();
        assertThat(Instant.parse(normalized.getTimeTo()))
                .isAfter(Instant.parse(normalized.getTimeFrom()));
    }

    @Test
    void resolvesVietnameseLongDateFromQuestion() {
        SearchRequest request = SearchRequest.builder()
                .question("show events trong ngay 7 thang 12 nam 2021")
                .build();

        SearchIntent normalized = normalizer.normalize(request, SearchIntent.builder().build());

        assertThat(normalized.getTimeFrom()).isEqualTo("2021-12-07T00:00:00Z");
        assertThat(normalized.getTimeTo()).isEqualTo("2021-12-08T00:00:00Z");
    }

    @Test
    void keepsAmbiguousDayMonthAsSpanWithoutInventingRange() {
        SearchRequest request = SearchRequest.builder()
                .question("show events trong ngay 7 thang 12")
                .build();

        SearchIntent normalized = normalizer.normalize(request, SearchIntent.builder().build());

        assertThat(normalized.getTimeFrom()).isNull();
        assertThat(normalized.getTimeTo()).isNull();
        assertThat(normalized.getSemanticSpans())
                .anySatisfy(span -> {
                    assertThat(span.kind()).isEqualTo(SemanticSpan.Kind.TEMPORAL);
                    assertThat(span.status()).isEqualTo(SemanticSpan.Status.AMBIGUOUS);
                });
    }
}
