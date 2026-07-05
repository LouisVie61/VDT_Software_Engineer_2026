package vdt.se.demo.adapter.out.llm;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.application.service.llm.LlmToolDefinitions;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IqlSystemPromptBuilderTest {
    @Test
    void identifiesDerivedTimeFieldsAndRejectsRawTimestampGrouping() {
        String prompt = new IqlSystemPromptBuilder(new ObjectMapper()).systemPrompt(List.of());

        assertThat(prompt)
                .contains("timestamp_year, timestamp_quarter, timestamp_month, timestamp_day, timestamp_hour")
                .contains("timestamp is not groupable")
                .contains("day/ngày=>timestamp_day");
    }

    @Test
    void embedsAuthoritativeToolSchemasAndGroupingSemantics() {
        ObjectMapper mapper = new ObjectMapper();
        String prompt = new IqlSystemPromptBuilder(mapper)
                .systemPrompt(new LlmToolDefinitions(mapper).all());

        assertThat(prompt)
                .contains("Authoritative supplied tool schemas")
                .contains("\"name\":\"search_events\"")
                .contains("For grouped counts, emit group_by plus metrics")
                .contains("size is event-hit count")
                .contains("Pagination is server-managed");
    }

    @Test
    void definesBilingualSemanticInterpretationWithoutTranslatingSchemaNames() {
        String prompt = new IqlSystemPromptBuilder(new ObjectMapper()).systemPrompt(List.of());

        assertThat(prompt)
                .contains("English, Vietnamese, and mixed English-Vietnamese")
                .contains("count/how many = đếm/bao nhiêu/số lượng")
                .contains("Infer equivalent natural phrasing from context".toLowerCase())
                .contains("Vietnamese request => Vietnamese question")
                .contains("schema names in tool arguments");
    }

    @Test
    void plansDependentDualIntentsAsNestedSelectButEmitsOneValidToolCall() {
        String prompt = new IqlSystemPromptBuilder(new ObjectMapper()).systemPrompt(List.of());

        assertThat(prompt)
                .contains("Nested SELECT planning DSL (reasoning only)")
                .contains("SELECT <outer result>")
                .contains("schema-valid search_events call")
                .contains("A single call cannot")
                .contains("Never emit a fictional nested query")
                .contains("documented $ref object");
    }
}
