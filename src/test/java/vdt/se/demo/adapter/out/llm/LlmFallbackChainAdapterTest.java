package vdt.se.demo.adapter.out.llm;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.adapter.config.AppProperties;
import vdt.se.demo.application.dto.IntentExtractionRequest;
import vdt.se.demo.application.dto.SearchRequest;
import vdt.se.demo.application.port.outboundPort.llm.LlmProviderPort;
import vdt.se.demo.domain.exception.LlmRetryableException;
import vdt.se.demo.domain.model.ExtractedIntent;
import vdt.se.demo.domain.model.ExecutionResult;
import vdt.se.demo.domain.model.RoutingHint;
import vdt.se.demo.domain.valueObjects.LlmProvider;
import vdt.se.demo.domain.valueObjects.TemplateType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LlmFallbackChainAdapterTest {

    @Test
    void fallsBackFromGeminiToGroqAndReturnsExtractedIntent() {
        LlmFallbackChainAdapter chain = chain(List.of(failing(LlmProvider.GEMINI), successfulGroq()));
        SearchRequest request = request("show failed login");

        ExtractedIntent response = chain.extract(extraction(request, TemplateType.SIMPLE_SEARCH));

        assertThat(response.provider()).isEqualTo("GROQ");
        assertThat(response.intent().getFilters()).containsEntry("event_type", "auth");
    }

    @Test
    void usesLocalFallbackIntentWhenAllProvidersFail() {
        LlmFallbackChainAdapter chain = chain(List.of(failing(LlmProvider.GEMINI), failing(LlmProvider.GROQ)));

        ExtractedIntent response = chain.extract(extraction(request("show failed login"), TemplateType.SIMPLE_SEARCH));

        assertThat(response.provider()).isEqualTo("LOCAL_FALLBACK");
        assertThat(response.intent().getTextQuery()).contains("failed");
        assertThat(response.intent().getFilters()).containsEntry("event_type", "auth");
    }

    @Test
    void returnsDeterministicSummaryWhenProvidersFail() {
        LlmFallbackChainAdapter chain = chain(List.of(failing(LlmProvider.GEMINI), failing(LlmProvider.GROQ)));

        String summary = chain.summarize(
                request("show failed login"),
                new ObjectMapper().readTree("{\"query\":{\"match_all\":{}}}"),
                ExecutionResult.builder()
                        .results(List.of())
                        .aggregations(List.of())
                        .totalCount(12)
                        .build()
        );

        assertThat(summary).contains("Found 12 matching events");
    }

    private LlmFallbackChainAdapter chain(List<LlmProviderPort> providers) {
        return new LlmFallbackChainAdapter(
                providers,
                new LlmIntentPromptBuilder(),
                new LlmSummaryPromptBuilder(),
                new LocalFallbackIntentExtractor(),
                new DeterministicSummaryBuilder(),
                new IntentResponseParser(new ObjectMapper()),
                new AppProperties()
        );
    }

    private IntentExtractionRequest extraction(SearchRequest request, TemplateType hint) {
        return IntentExtractionRequest.builder()
                .request(request)
                .routingHint(RoutingHint.builder()
                        .templateType(hint)
                        .confidence(0.8d)
                        .reason("test")
                        .semantic(false)
                        .build())
                .enrichments(List.of())
                .build();
    }

    private SearchRequest request(String question) {
        return SearchRequest.builder()
                .question(question)
                .build();
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
                return "{\"intent\":\"SIMPLE_SEARCH\",\"textQuery\":\"failed login\",\"filters\":{\"event_type\":\"auth\"}}";
            }
        };
    }
}
