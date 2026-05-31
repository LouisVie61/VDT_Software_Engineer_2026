package vdt.se.demo.adapter.out.llm;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.adapter.config.AppProperties;
import vdt.se.demo.application.dto.SearchRequest;
import vdt.se.demo.application.port.outboundPort.LlmProviderAdapter;
import vdt.se.demo.application.port.outboundPort.LlmResponse;
import vdt.se.demo.domain.exception.LlmException;
import vdt.se.demo.domain.exception.LlmRetryableException;
import vdt.se.demo.domain.model.ExecutionResult;
import vdt.se.demo.domain.valueObjects.LlmProvider;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LlmFallbackChainAdapterTest {

    @Test
    void fallsBackFromGeminiToGroqAndReturnsGeneratedDsl() {
        AppProperties properties = new AppProperties();
        LlmFallbackChainAdapter chain = new LlmFallbackChainAdapter(
                List.of(failing(LlmProvider.GEMINI), successfulGroq()),
                new LlmPromptBuilder(),
                new DslResponseParser(new ObjectMapper()),
                properties
        );
        SearchRequest request = new SearchRequest();
        request.setQuestion("show failed login");

        LlmResponse response = chain.generateDsl(request);

        assertThat(response.provider()).isEqualTo("GROQ");
        assertThat(response.generatedDsl().has("query")).isTrue();
    }

    @Test
    void throwsWhenAllProvidersFailInsteadOfUsingHeuristic() {
        AppProperties properties = new AppProperties();
        LlmFallbackChainAdapter chain = new LlmFallbackChainAdapter(
                List.of(failing(LlmProvider.GEMINI), failing(LlmProvider.GROQ)),
                new LlmPromptBuilder(),
                new DslResponseParser(new ObjectMapper()),
                properties
        );
        SearchRequest request = new SearchRequest();
        request.setQuestion("show failed login");

        assertThatThrownBy(() -> chain.generateDsl(request))
                .isInstanceOf(LlmException.class)
                .hasMessageContaining("All LLM providers failed");
    }

    @Test
    void returnsDeterministicSummaryWhenProvidersFail() {
        AppProperties properties = new AppProperties();
        LlmFallbackChainAdapter chain = new LlmFallbackChainAdapter(
                List.of(failing(LlmProvider.GEMINI), failing(LlmProvider.GROQ)),
                new LlmPromptBuilder(),
                new DslResponseParser(new ObjectMapper()),
                properties
        );
        SearchRequest request = new SearchRequest();
        request.setQuestion("show failed login");

        String summary = chain.summarize(request, new ObjectMapper().readTree("{\"query\":{\"match_all\":{}}}"),
                new ExecutionResult(List.of(), List.of(), 12));

        assertThat(summary).contains("Found 12 matching events");
    }

    private LlmProviderAdapter failing(LlmProvider provider) {
        return new LlmProviderAdapter() {
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

    private LlmProviderAdapter successfulGroq() {
        return new LlmProviderAdapter() {
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
