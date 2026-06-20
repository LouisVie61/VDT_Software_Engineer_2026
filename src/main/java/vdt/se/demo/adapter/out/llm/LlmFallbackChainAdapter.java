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
        String prompt = intentPromptBuilder.build(request);
        RuntimeException lastFailure = null;
        for (LlmProvider provider : providerOrder()) {
            LlmProviderPort providerPort = find(provider);
            if (providerPort == null) {
                continue;
            }
            try {
                String raw = providerPort.complete(prompt);
                SearchIntent intent = intentResponseParser.parse(raw);
                log.debug("LLM provider {} extracted valid search intent fields", provider);
                return ExtractedIntent.builder()
                        .intent(intent)
                        .provider(provider.name())
                        .rawContent(raw)
                        .build();
            } catch (LlmRetryableException e) {
                lastFailure = e;
                log.debug("LLM provider {} failed to extract valid intent: {}", provider, e.getMessage());
            }
        }
        log.debug("All LLM providers failed to extract intent. Falling back to local deterministic extraction. Last error: {}",
                lastFailure == null ? "none" : lastFailure.getMessage());
        SearchIntent fallback = localFallbackIntentExtractor.extract(request.request(), request.routingHint());
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
        String prompt = summaryPromptBuilder.build(request, generatedDsl, executionResult);
        for (LlmProvider provider : providerOrder()) {
            LlmProviderPort providerPort = find(provider);
            if (providerPort == null) {
                continue;
            }
            try {
                String summary = providerPort.complete(prompt);
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
