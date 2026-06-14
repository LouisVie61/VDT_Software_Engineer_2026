package vdt.se.demo.adapter.out.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import vdt.se.demo.adapter.config.AppProperties;
import vdt.se.demo.application.dto.SearchRequest;
import vdt.se.demo.application.port.outboundPort.LlmFallbackChain;
import vdt.se.demo.application.port.outboundPort.LlmProviderPort;
import vdt.se.demo.application.port.outboundPort.LlmResponse;
import vdt.se.demo.domain.model.ExecutionResult;
import vdt.se.demo.domain.valueObjects.LlmProvider;

import java.util.List;
import java.util.Locale;

@Component
public class LlmFallbackChainAdapter implements LlmFallbackChain {

    private static final Logger log = LoggerFactory.getLogger(LlmFallbackChainAdapter.class);

    private final List<LlmProviderPort> providers;
    private final LlmDslPromptBuilder dslPromptBuilder;
    private final LlmSummaryPromptBuilder summaryPromptBuilder;
    private final LocalFallbackDslBuilder localFallbackDslBuilder;
    private final DeterministicSummaryBuilder deterministicSummaryBuilder;
    private final DslResponseParser parser;
    private final AppProperties properties;

    public LlmFallbackChainAdapter(List<LlmProviderPort> providers, LlmDslPromptBuilder dslPromptBuilder,
                                   LlmSummaryPromptBuilder summaryPromptBuilder,
                                   LocalFallbackDslBuilder localFallbackDslBuilder,
                                   DeterministicSummaryBuilder deterministicSummaryBuilder,
                                   DslResponseParser parser, AppProperties properties) {
        this.providers = providers;
        this.dslPromptBuilder = dslPromptBuilder;
        this.summaryPromptBuilder = summaryPromptBuilder;
        this.localFallbackDslBuilder = localFallbackDslBuilder;
        this.deterministicSummaryBuilder = deterministicSummaryBuilder;
        this.parser = parser;
        this.properties = properties;
    }

    @Override
    public LlmResponse generateDsl(SearchRequest request) {
        String prompt = dslPromptBuilder.build(request);
        RuntimeException lastFailure = null;
        for (LlmProvider provider : providerOrder()) {
            LlmProviderPort providerPort = find(provider);
            if (providerPort == null) {
                continue;
            }
            try {
                String raw = providerPort.complete(prompt);
                JsonNode generatedDsl = parser.parse(raw);
                log.info("LLM provider {} generated valid Elasticsearch DSL", provider);
                return new LlmResponse(provider.name(), generatedDsl, raw);
            } catch (RuntimeException e) {
                lastFailure = e;
                log.warn("LLM provider {} failed to generate valid Elasticsearch DSL: {}", provider, e.getMessage());
            }
        }
        log.warn("All LLM providers failed to generate DSL. Falling back to local keyword DSL. Last error: {}",
                lastFailure == null ? "none" : lastFailure.getMessage());
        String raw = localFallbackDslBuilder.build(request);
        return new LlmResponse("LOCAL_FALLBACK", parser.parse(raw), raw);
    }

    @Override
    public String summarize(SearchRequest request, JsonNode generatedDsl, ExecutionResult executionResult) {
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
            } catch (RuntimeException ignored) {
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
