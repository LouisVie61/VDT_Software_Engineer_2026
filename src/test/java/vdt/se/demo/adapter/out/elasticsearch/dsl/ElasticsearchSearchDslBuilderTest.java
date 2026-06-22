package vdt.se.demo.adapter.out.elasticsearch.dsl;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.application.dto.SearchRequest;
import vdt.se.demo.domain.model.CanonicalQueryPlan;
import vdt.se.demo.domain.model.SearchIntent;
import vdt.se.demo.domain.model.TemplateSelection;
import vdt.se.demo.domain.valueObjects.ChartType;
import vdt.se.demo.domain.valueObjects.TemplateType;

import static org.assertj.core.api.Assertions.assertThat;

class ElasticsearchSearchDslBuilderTest {

    private final ElasticsearchSearchDslBuilder builder = new ElasticsearchSearchDslBuilder(new ObjectMapper());

    @Test
    void buildsQuarterTimeAggregationWithEventHitsRawOrWholeQuestionText() {
        SearchRequest request = SearchRequest.builder()
                .question("Thong ke loi theo quy cua nam 2023")
                .page(2)
                .pageSize(50)
                .build();
        SearchIntent intent = SearchIntent.builder()
                .intent(TemplateType.TIME_AGGREGATION)
                .timeBucket("quarter")
                .timeFrom("2023-01-01T00:00:00Z")
                .timeTo("2024-01-01T00:00:00Z")
                .build();

        JsonNode dsl = builder.build(request, plan(intent, TemplateType.TIME_AGGREGATION, "timestamp"));
        String compact = dsl.toString();

        assertThat(dsl.get("size").asInt()).isEqualTo(50);
        assertThat(dsl.path("from").asInt()).isEqualTo(100);
        assertThat(dsl.path("track_total_hits").asBoolean()).isTrue();
        assertThat(dsl.has("sort")).isTrue();
        assertThat(dsl.path("aggs").path("events_over_time").path("date_histogram").path("field").asString())
                .isEqualTo("timestamp");
        assertThat(dsl.path("aggs").path("events_over_time").path("date_histogram").path("calendar_interval").asString())
                .isEqualTo("quarter");
        assertThat(compact).doesNotContain("thong ke loi theo quy cua nam 2023");
        assertThat(compact).doesNotContain("\"raw\"");
        assertThat(compact).doesNotContain("top_values");
    }

    @Test
    void buildsTermsAggregationWithScrollableHits() {
        SearchRequest request = SearchRequest.builder()
                .question("top 10 ip")
                .page(2)
                .pageSize(50)
                .severity("Critical")
                .build();
        SearchIntent intent = SearchIntent.builder()
                .intent(TemplateType.TERMS_AGGREGATION)
                .groupBy("ip")
                .topN(10)
                .build();

        JsonNode dsl = builder.build(request, plan(intent, TemplateType.TERMS_AGGREGATION, "ip"));

        assertThat(dsl.get("size").asInt()).isEqualTo(50);
        assertThat(dsl.path("from").asInt()).isEqualTo(100);
        assertThat(dsl.has("sort")).isTrue();
        assertThat(dsl.toString()).contains("\"term\":{\"severity\":\"critical\"}");
        assertThat(dsl.toString()).doesNotContain("\"severity\":\"Critical\"");
        assertThat(dsl.path("aggs").path("top_values").path("terms").path("field").asString()).isEqualTo("ip");
    }

    @Test
    void buildsWeeklyTimeAggregationWithCalendarInterval() {
        SearchRequest request = SearchRequest.builder()
                .question("statistics event per week in 2022")
                .page(0)
                .pageSize(50)
                .build();
        SearchIntent intent = SearchIntent.builder()
                .intent(TemplateType.TIME_AGGREGATION)
                .timeBucket("1w")
                .timeFrom("2022-01-01T00:00:00Z")
                .timeTo("2023-01-01T00:00:00Z")
                .build();

        JsonNode dsl = builder.build(request, plan(intent, TemplateType.TIME_AGGREGATION, "timestamp"));
        JsonNode histogram = dsl.path("aggs").path("events_over_time").path("date_histogram");

        assertThat(histogram.path("calendar_interval").asString()).isEqualTo("week");
        assertThat(histogram.has("fixed_interval")).isFalse();
    }

    @Test
    void fullTextSearchUsesMessageOnly() {
        SearchRequest request = SearchRequest.builder()
                .question("connection timeout")
                .page(0)
                .pageSize(50)
                .build();
        SearchIntent intent = SearchIntent.builder()
                .intent(TemplateType.SIMPLE_SEARCH)
                .textQuery("connection timeout")
                .build();

        JsonNode dsl = builder.build(request, plan(intent, TemplateType.SIMPLE_SEARCH, null));
        JsonNode fields = dsl.path("query").path("bool").path("must").path(0)
                .path("simple_query_string").path("fields");

        assertThat(fields.toString()).isEqualTo("[\"message\"]");
        assertThat(dsl.toString()).doesNotContain("\"raw\"");
    }

    @Test
    void buildsSimpleSearchWithEveryExplicitFilter() {
        SearchRequest request = SearchRequest.builder()
                .question("show filtered events")
                .page(0)
                .pageSize(25)
                .from("2026-01-01T00:00:00Z")
                .to("2026-01-31T23:59:59Z")
                .severity("Critical")
                .eventType("IDS_ALERT")
                .user("alice")
                .host("host-1")
                .ip("10.0.0.1")
                .build();
        SearchIntent intent = SearchIntent.builder()
                .intent(TemplateType.SIMPLE_SEARCH)
                .build();

        JsonNode dsl = builder.build(request, plan(intent, TemplateType.SIMPLE_SEARCH, null));
        String compact = dsl.toString();

        assertThat(compact).contains("\"term\":{\"severity\":\"critical\"}");
        assertThat(compact).contains("\"term\":{\"event_type\":\"ids_alert\"}");
        assertThat(compact).contains("\"term\":{\"user\":\"alice\"}");
        assertThat(compact).contains("\"term\":{\"host\":\"host-1\"}");
        assertThat(compact).contains("\"term\":{\"ip\":\"10.0.0.1\"}");
        assertThat(compact).contains("\"gte\":\"2026-01-01T00:00:00Z\"");
        assertThat(compact).contains("\"lte\":\"2026-01-31T23:59:59Z\"");
    }

    private CanonicalQueryPlan plan(SearchIntent intent, TemplateType type, String groupBy) {
        return CanonicalQueryPlan.builder()
                .mergedIntent(intent)
                .templateSelection(TemplateSelection.builder()
                        .type(type)
                        .groupBy(groupBy)
                        .size(intent.getTopN() == null ? 10 : intent.getTopN())
                        .chartHint(type == TemplateType.TIME_AGGREGATION ? ChartType.LINE_CHART : ChartType.TABLE)
                        .reason("test")
                        .build())
                .build();
    }
}
