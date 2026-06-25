package vdt.se.demo.adapter.out.llm;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.domain.model.SearchIntent;
import vdt.se.demo.domain.model.SemanticSpan;

import static org.assertj.core.api.Assertions.assertThat;

class IntentResponseParserTest {

    private final IntentResponseParser parser = new IntentResponseParser(new ObjectMapper());

    @Test
    void parsesSemanticSpansFromLlmIntentResponse() {
        SearchIntent intent = parser.parse("""
                {
                  "intent": "SIMPLE_SEARCH",
                  "textQuery": "failed",
                  "filters": {"event_type": "auth"},
                  "semanticSpans": [
                    {"kind": "TEMPORAL", "status": "RESOLVED", "text": "2025", "canonical": "year_2025", "start": 10, "end": 14},
                    {"kind": "FILTER", "status": "AMBIGUOUS", "text": "attack"}
                  ],
                  "confidenceScores": {"textQuery": 0.8}
                }
                """);

        assertThat(intent.getSemanticSpans()).hasSize(2);
        assertThat(intent.getSemanticSpans().getFirst().kind()).isEqualTo(SemanticSpan.Kind.TEMPORAL);
        assertThat(intent.getSemanticSpans().getFirst().status()).isEqualTo(SemanticSpan.Status.RESOLVED);
        assertThat(intent.getSemanticSpans().getFirst().canonical()).isEqualTo("year_2025");
        assertThat(intent.getSemanticSpans().get(1).status()).isEqualTo(SemanticSpan.Status.AMBIGUOUS);
    }
}
