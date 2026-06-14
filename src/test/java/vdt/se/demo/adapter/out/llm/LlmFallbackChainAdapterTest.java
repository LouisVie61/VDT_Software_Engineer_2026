package vdt.se.demo.adapter.out.llm;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.adapter.config.AppProperties;
import vdt.se.demo.application.dto.SearchRequest;
import vdt.se.demo.application.port.outboundPort.LlmProviderPort;
import vdt.se.demo.application.port.outboundPort.LlmResponse;
import vdt.se.demo.domain.exception.LlmRetryableException;
import vdt.se.demo.domain.model.ExecutionResult;
import vdt.se.demo.domain.valueObjects.LlmProvider;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LlmFallbackChainAdapterTest {

    @Test
    void fallsBackFromGeminiToGroqAndReturnsGeneratedDsl() {
        LlmFallbackChainAdapter chain = chain(List.of(failing(LlmProvider.GEMINI), successfulGroq()));
        SearchRequest request = request("show failed login");

        LlmResponse response = chain.generateDsl(request);

        assertThat(response.provider()).isEqualTo("GROQ");
        assertThat(response.generatedDsl().has("query")).isTrue();
    }

    @Test
    void usesLocalFallbackDslWhenAllProvidersFail() {
        LlmFallbackChainAdapter chain = chain(List.of(failing(LlmProvider.GEMINI), failing(LlmProvider.GROQ)));

        LlmResponse response = chain.generateDsl(request("show failed login"));

        assertThat(response.provider()).isEqualTo("LOCAL_FALLBACK");
        assertThat(response.generatedDsl().get("query").get("bool").get("must").get(0)
                .get("simple_query_string").get("query").asString()).contains("failed login auth");
        assertThat(response.rawContent()).contains("\"default_operator\": \"or\"");
    }

    @Test
    void returnsDeterministicSummaryWhenProvidersFail() {
        LlmFallbackChainAdapter chain = chain(List.of(failing(LlmProvider.GEMINI), failing(LlmProvider.GROQ)));

        String summary = chain.summarize(
                request("show failed login"),
                new ObjectMapper().readTree("{\"query\":{\"match_all\":{}}}"),
                new ExecutionResult(List.of(), List.of(), 12)
        );

        assertThat(summary).contains("Found 12 matching events");
    }

    private LlmFallbackChainAdapter chain(List<LlmProviderPort> providers) {
        return new LlmFallbackChainAdapter(
                providers,
                new LlmDslPromptBuilder(),
                new LlmSummaryPromptBuilder(),
                new LocalFallbackDslBuilder(),
                new DeterministicSummaryBuilder(),
                new DslResponseParser(new ObjectMapper()),
                new AppProperties()
        );
    }

    private SearchRequest request(String question) {
        SearchRequest request = new SearchRequest();
        request.setQuestion(question);
        return request;
    }

    private LlmProviderPort failing(LlmProvider provider) {
        return new LlmProviderPort() {
            @Override
            public LlmProvider provider() {
                return provider;
            }

            @Override
            public String complete(String prompt) {
                throw new LlmRetryableException("down");
            }
        };
    }

    private LlmProviderPort successfulGroq() {
        return new LlmProviderPort() {
            @Override
            public LlmProvider provider() {
                return LlmProvider.GROQ;
            }

            @Override
            public String complete(String prompt) {
                return "{\"query\":{\"match_all\":{}},\"size\":10}";
            }
        };
    }
}
