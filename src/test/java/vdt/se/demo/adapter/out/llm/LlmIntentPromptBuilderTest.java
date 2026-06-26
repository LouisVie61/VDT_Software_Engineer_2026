package vdt.se.demo.adapter.out.llm;

import org.junit.jupiter.api.Test;
import vdt.se.demo.application.dto.IntentExtractionRequest;
import vdt.se.demo.application.dto.SearchRequest;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class LlmIntentPromptBuilderTest {

    @Test
    void promptMapsLocationLanguageToGeoLocationOnly() {
        SearchRequest searchRequest = SearchRequest.builder()
                .question("show events at Vietnam")
                .build();
        IntentExtractionRequest request = IntentExtractionRequest.builder()
                .request(searchRequest)
                .build();

        String prompt = new LlmIntentPromptBuilder().build(request);

        assertThat(prompt).contains("filters.geo_location");
        assertThat(prompt).contains("Never output filters named location, country, city, province, region, state, or address");
        assertThat(prompt).contains("at Vietnam");
        assertThat(prompt).contains("filters.geo_location = \"Vietnam\"");
    }

    @Test
    void promptFormattingKeepsCurrentDatetimeInNowSlot() {
        SearchRequest searchRequest = SearchRequest.builder()
                .question("show events")
                .build();
        IntentExtractionRequest request = IntentExtractionRequest.builder()
                .request(searchRequest)
                .build();

        String prompt = new LlmIntentPromptBuilder().build(request);

        assertThat(prompt).containsPattern(Pattern.compile("Now: \\d{4}-\\d{2}-\\d{2}T"));
        assertThat(prompt).contains("=== ALLOWED FIELDS ===\n- timestamp: date");
    }

    @Test
    void promptDoesNotInferEventTypeGroupByFromStatisticsEventsAlone() {
        SearchRequest searchRequest = SearchRequest.builder()
                .question("Statistics events from June 1 to June 15 2025")
                .build();
        IntentExtractionRequest request = IntentExtractionRequest.builder()
                .request(searchRequest)
                .build();

        String prompt = new LlmIntentPromptBuilder().build(request);

        assertThat(prompt).contains("Do not infer groupBy=event_type merely because the user asks for statistics/events.");
        assertThat(prompt).contains("If the user asks for statistics over a time period without explicit grouping, prefer");
        assertThat(prompt).doesNotContain("- statistics -> event_type");
    }

    @Test
    void promptTreatsAnnualMonthAsRecurrenceNotNextCalendarMonth() {
        SearchRequest searchRequest = SearchRequest.builder()
                .question("thong ke su kien trong thang 7 hang nam")
                .build();
        IntentExtractionRequest request = IntentExtractionRequest.builder()
                .request(searchRequest)
                .build();

        String prompt = new LlmIntentPromptBuilder().build(request);

        assertThat(prompt).contains("annual recurring");
        assertThat(prompt).contains("Do not resolve them to the next/current calendar year such as July 2026.");
        assertThat(prompt).contains("recurringTime={\"mode\":\"EVERY_YEAR\",\"month\":7}");
        assertThat(prompt).contains("Use geo_location only for an explicit location constraint.");
    }
}
