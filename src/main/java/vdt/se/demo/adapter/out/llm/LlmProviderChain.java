package vdt.se.demo.adapter.out.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vdt.se.demo.adapter.config.AppProperties;
import vdt.se.demo.application.port.outboundPort.llm.LlmProviderPort;
import vdt.se.demo.domain.exception.LlmException;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

final class LlmProviderChain {
    private static final Logger log = LoggerFactory.getLogger(LlmProviderChain.class);
    private final List<LlmProviderPort> providers;

    LlmProviderChain(List<LlmProviderPort> providers, AppProperties properties) {
        List<String> order = List.of(properties.getLlm().getProviderOrder().split(",")).stream()
                .map(String::trim).filter(value -> !value.isBlank())
                .map(value -> value.toUpperCase(Locale.ROOT)).toList();
        this.providers = providers.stream().sorted(Comparator.comparingInt(provider -> {
            int index = order.indexOf(provider.provider().name());
            return index < 0 ? Integer.MAX_VALUE : index;
        })).toList();
    }

    <T> T firstSuccessful(Function<LlmProviderPort, T> call) {
        RuntimeException last = null;
        for (LlmProviderPort provider : providers) {
            try { return call.apply(provider); }
            catch (RuntimeException failure) {
                last = failure;
                log.warn("LLM provider failed: provider={}, reason={}", provider.provider(), failure.getMessage());
            }
        }
        throw new LlmException("No LLM provider produced a valid IQL tool call", last);
    }
}
