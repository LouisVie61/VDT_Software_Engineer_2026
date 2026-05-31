package vdt.se.demo.adapter.out.llm;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import vdt.se.demo.adapter.config.AppProperties;
import vdt.se.demo.application.dto.SearchRequest;
import vdt.se.demo.application.port.outboundPort.LlmFallbackChain;
import vdt.se.demo.application.port.outboundPort.LlmProviderAdapter;
import vdt.se.demo.application.port.outboundPort.LlmResponse;
import vdt.se.demo.domain.exception.LlmException;
import vdt.se.demo.domain.model.ExecutionResult;
import vdt.se.demo.domain.valueObjects.LlmProvider;

import java.util.List;
import java.util.Locale;

@Component
public class LlmFallbackChainAdapter implements LlmFallbackChain {

    private final List<LlmProviderAdapter> adapters;
    private final LlmPromptBuilder promptBuilder;
    private final DslResponseParser parser;
    private final AppProperties properties;

    public LlmFallbackChainAdapter(List<LlmProviderAdapter> adapters, LlmPromptBuilder promptBuilder,
                                   DslResponseParser parser, AppProperties properties) {
        this.adapters = adapters;
        this.promptBuilder = promptBuilder;
        this.parser = parser;
        this.properties = properties;
    }

    @Override
    public LlmResponse generateDsl(SearchRequest request) {
        String prompt = promptBuilder.buildDslPrompt(request);
        RuntimeException lastFailure = null;
        for (LlmProvider provider : providerOrder()) {
            LlmProviderAdapter adapter = find(provider);
            if (adapter == null) {
                continue;
            }
            try {
                String raw = adapter.complete(prompt);
                JsonNode generatedDsl = parser.parse(raw);
                return new LlmResponse(provider.name(), generatedDsl, raw);
            } catch (RuntimeException e) {
                lastFailure = e;
            }
        }
        throw new LlmException("All LLM providers failed to generate valid Elasticsearch DSL", lastFailure);
    }

    @Override
    public String summarize(SearchRequest request, JsonNode generatedDsl, ExecutionResult executionResult) {
        String prompt = promptBuilder.buildSummaryPrompt(request, generatedDsl, executionResult);
        for (LlmProvider provider : providerOrder()) {
            LlmProviderAdapter adapter = find(provider);
            if (adapter == null) {
                continue;
            }
            try {
                String summary = adapter.complete(prompt);
                if (summary != null && !summary.isBlank()) {
                    return summary.strip();
                }
            } catch (RuntimeException ignored) {
                // Deterministic summary below is acceptable when providers are unavailable.
            }
        }
        return "Found " + executionResult.totalCount() + " matching events. "
                + "Review the generated query and the highest-frequency aggregation buckets for investigation. "
                + "Start by checking repeated users, hosts, or IP addresses in the returned results.";
    }

    private List<LlmProvider> providerOrder() {
        return List.of(properties.getLlm().getProviderOrder().split(",")).stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> LlmProvider.valueOf(value.toUpperCase(Locale.ROOT)))
                .toList();
    }

    private LlmProviderAdapter find(LlmProvider provider) {
        return adapters.stream()
                .filter(adapter -> adapter.provider() == provider)
                .findFirst()
                .orElse(null);
    }
}
