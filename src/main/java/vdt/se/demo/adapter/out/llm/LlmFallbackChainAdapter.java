package vdt.se.demo.adapter.out.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import vdt.se.demo.adapter.config.AppProperties;
import vdt.se.demo.application.dto.IntentExtractionRequest;
import vdt.se.demo.application.dto.SearchRequest;
import vdt.se.demo.application.port.outboundPort.llm.IntentExtractionPort;
import vdt.se.demo.application.port.outboundPort.llm.LlmProviderPort;
import vdt.se.demo.application.port.outboundPort.llm.SummaryPort;
import vdt.se.demo.domain.model.ExtractedIntent;
import vdt.se.demo.domain.model.ExecutionResult;
import vdt.se.demo.domain.model.SearchIntent;
import vdt.se.demo.domain.exception.LlmException;
import vdt.se.demo.domain.exception.LlmRetryableException;
import vdt.se.demo.domain.valueObjects.LlmProvider;

import java.util.List;
import java.util.Locale;

@Component
public class LlmFallbackChainAdapter implements IntentExtractionPort, SummaryPort {

    private static final Logger log = LoggerFactory.getLogger(LlmFallbackChainAdapter.class);

    private final List<LlmProviderPort> providers;
    private final LlmIntentPromptBuilder intentPromptBuilder;
    private final LlmSummaryPromptBuilder summaryPromptBuilder;
    private final LocalFallbackIntentExtractor localFallbackIntentExtractor;
    private final DeterministicSummaryBuilder deterministicSummaryBuilder;
    private final IntentResponseParser intentResponseParser;
    private final AppProperties properties;

    public LlmFallbackChainAdapter(List<LlmProviderPort> providers, LlmIntentPromptBuilder intentPromptBuilder,
                                   LlmSummaryPromptBuilder summaryPromptBuilder,
                                   LocalFallbackIntentExtractor localFallbackIntentExtractor,
                                   DeterministicSummaryBuilder deterministicSummaryBuilder,
                                   IntentResponseParser intentResponseParser, AppProperties properties) {
        this.providers = providers;
        this.intentPromptBuilder = intentPromptBuilder;
        this.summaryPromptBuilder = summaryPromptBuilder;
        this.localFallbackIntentExtractor = localFallbackIntentExtractor;
        this.deterministicSummaryBuilder = deterministicSummaryBuilder;
        this.intentResponseParser = intentResponseParser;
        this.properties = properties;
    }

    @Override
    public ExtractedIntent extract(IntentExtractionRequest request) {
        String systemPrompt = intentPromptBuilder.systemPrompt();
        String prompt = intentPromptBuilder.build(request);
        log.info("[INTENT_EXTRACTION] Processing question: {}", request.request().getQuestion());
        log.debug("[INTENT_EXTRACTION] System prompt length: {}", systemPrompt.length());
        log.debug("[INTENT_EXTRACTION] User prompt length: {}", prompt.length());
        Exception lastFailure = null;
        for (LlmProvider provider : providerOrder()) {
            LlmProviderPort providerPort = find(provider);
            if (providerPort == null) {
                continue;
            }
            try {
                log.info("[INTENT_EXTRACTION] Invoking LLM provider: {}", provider);
                String raw = providerPort.complete(systemPrompt, prompt);
                log.info("[INTENT_EXTRACTION] LLM raw response from {}: {}", provider, raw);
                SearchIntent intent = intentResponseParser.parse(raw);
                log.info("[INTENT_EXTRACTION] Successfully parsed intent from {}: intent={}, groupBy={}, topN={}, textQuery={}",
                        provider, intent.getIntent(), intent.getGroupBy(), intent.getTopN(), intent.getTextQuery());
                return ExtractedIntent.builder()
                        .intent(intent)
                        .provider(provider.name())
                        .rawContent(raw)
                        .build();
            } catch (LlmRetryableException e) {
                lastFailure = e;
                log.warn("[INTENT_EXTRACTION] LLM provider {} unavailable or timed out: {}", provider, e.getMessage());
            } catch (LlmException e) {
                lastFailure = e;
                log.warn("[INTENT_EXTRACTION] LLM provider {} returned an unusable response: {}", provider, e.getMessage(), e);
            } catch (Exception e) {
                lastFailure = e;
                log.warn("[INTENT_EXTRACTION] Unexpected error from LLM provider {}: {}", provider, e.getMessage(), e);
            }
        }
        log.warn("[INTENT_EXTRACTION] All LLM providers failed. Using local deterministic fallback. Last error: {}",
                lastFailure == null ? "none" : lastFailure.getMessage());
        SearchIntent fallback = localFallbackIntentExtractor.extract(request.request(), request.routingHint());
        log.info("[INTENT_EXTRACTION] Fallback intent generated: intent={}, groupBy={}, topN={}",
                fallback.getIntent(), fallback.getGroupBy(), fallback.getTopN());
        return ExtractedIntent.builder()
                .intent(fallback)
                .provider("LOCAL_FALLBACK")
                .rawContent(fallback.toString())
                .build();
    }

    @Override
    public String summarize(SearchRequest request, JsonNode generatedDsl, ExecutionResult executionResult) {
        if (!properties.getLlm().isSummaryEnabled()) {
            return deterministicSummaryBuilder.build(executionResult);
        }
        String systemPrompt = summaryPromptBuilder.systemPrompt();
        String prompt = summaryPromptBuilder.build(request, generatedDsl, executionResult);
        for (LlmProvider provider : providerOrder()) {
            LlmProviderPort providerPort = find(provider);
            if (providerPort == null) {
                continue;
            }
            try {
                String summary = providerPort.complete(systemPrompt, prompt);
                if (summary != null && !summary.isBlank()) {
                    return summary.strip();
                }
            } catch (LlmRetryableException ignored) {
                // Deterministic summary is acceptable when providers are unavailable.
            }
        }
        return deterministicSummaryBuilder.build(executionResult);
    }

    private List<LlmProvider> providerOrder() {
        return List.of(properties.getLlm().getProviderOrder().split(",")).stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> LlmProvider.valueOf(value.toUpperCase(Locale.ROOT)))
                .toList();
    }

    private LlmProviderPort find(LlmProvider provider) {
        return providers.stream()
                .filter(candidate -> candidate.provider() == provider)
                .findFirst()
                .orElse(null);
    }
}
