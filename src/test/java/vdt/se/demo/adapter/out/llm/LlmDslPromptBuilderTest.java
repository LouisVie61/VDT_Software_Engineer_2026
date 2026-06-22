package vdt.se.demo.adapter.out.llm;

import org.junit.jupiter.api.Test;
import vdt.se.demo.application.dto.SearchRequest;

import static org.assertj.core.api.Assertions.assertThat;

class LlmDslPromptBuilderTest {

    @Test
    void promptContainsExpectedDslExamplesAndJsonOnlyInstruction() {
        SearchRequest request = SearchRequest.builder()
                .question("Top 10 IP nhieu alert nhat")
                .build();

        String prompt = new LlmDslPromptBuilder().build(request);

        assertThat(prompt).contains("Return raw JSON only");
        assertThat(prompt).contains("Expected search DSL");
        assertThat(prompt).contains("Expected terms aggregation DSL");
        assertThat(prompt).contains("Perception/routing output is only a soft prior");
        assertThat(prompt).contains("timestamp: date");
        assertThat(prompt).contains("event_type: keyword");
        assertThat(prompt).contains("Top 10 IP nhieu alert nhat");
    }
}
