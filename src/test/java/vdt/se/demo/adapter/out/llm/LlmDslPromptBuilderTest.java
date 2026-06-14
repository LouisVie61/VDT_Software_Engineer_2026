package vdt.se.demo.adapter.out.llm;

import org.junit.jupiter.api.Test;
import vdt.se.demo.application.dto.SearchRequest;

import static org.assertj.core.api.Assertions.assertThat;

class LlmDslPromptBuilderTest {

    @Test
    void promptContainsExpectedDslExamplesAndJsonOnlyInstruction() {
        SearchRequest request = new SearchRequest();
        request.setQuestion("Top 10 IP nhieu alert nhat");

        String prompt = new LlmDslPromptBuilder().build(request);

        assertThat(prompt).contains("Return JSON only");
        assertThat(prompt).contains("Expected search DSL");
        assertThat(prompt).contains("Expected terms aggregation DSL");
        assertThat(prompt).contains("timestamp, source, severity, event_type, user, host, ip");
        assertThat(prompt).contains("Top 10 IP nhieu alert nhat");
    }
}
