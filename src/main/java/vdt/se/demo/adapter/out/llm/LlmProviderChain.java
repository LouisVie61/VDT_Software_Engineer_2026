package vdt.se.demo.adapter.out.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vdt.se.demo.adapter.config.AppProperties;
import vdt.se.demo.application.port.outboundPort.llm.LlmProviderPort;
import vdt.se.demo.application.port.outboundPort.llm.LlmCallBudget;
import vdt.se.demo.domain.exception.LlmException;
import vdt.se.demo.domain.exception.LlmRateLimitException;

import java.time.Clock;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

final class LlmProviderChain {
    private static final Logger log = LoggerFactory.getLogger(LlmProviderChain.class);
    private final List<LlmProviderPort> providers;
    private final RateLimitCircuitBreaker rateLimits;

    LlmProviderChain(List<LlmProviderPort> providers, AppProperties properties) {
        this(providers, properties, new RateLimitCircuitBreaker(
                properties.getLlm().getCircuitBreaker().getThreshold(),
                Duration.ofSeconds(properties.getLlm().getCircuitBreaker().getWindowSeconds()),
                Duration.ofSeconds(properties.getLlm().getCircuitBreaker().getCooldownSeconds()), Clock.systemUTC()));
    }

    LlmProviderChain(List<LlmProviderPort> providers, AppProperties properties, RateLimitCircuitBreaker rateLimits) {
        List<String> order = List.of(properties.getLlm().getProviderOrder().split(",")).stream()
                .map(String::trim).filter(value -> !value.isBlank())
                .map(value -> value.toUpperCase(Locale.ROOT)).toList();
        this.providers = providers.stream().sorted(Comparator.comparingInt(provider -> {
            int index = order.indexOf(provider.provider().name());
            return index < 0 ? Integer.MAX_VALUE : index;
        })).toList();
        this.rateLimits = rateLimits;
        log.info("LLM provider chain initialized: configuredOrder={}, effectiveOrder={}", order,
                this.providers.stream().map(provider -> provider.provider().name()).toList());
    }

    <T> T firstSuccessful(Function<LlmProviderPort, T> call) {
        return firstSuccessful(call, new LlmCallBudget(Math.max(1, providers.size())));
    }

    <T> T firstSuccessful(Function<LlmProviderPort, T> call, LlmCallBudget budget) {
        RuntimeException last = null;
        for (LlmProviderPort provider : providers) {
            if (!rateLimits.permits(provider.provider())) {
                log.info("Skipping rate-limited LLM provider during cooldown: provider={}", provider.provider());
                continue;
            }
            // Count actual outbound requests, not logical correction rounds. Otherwise one
            // round can consume every configured provider while only charging the budget once.
            if (!budget.tryConsume()) {
                log.warn("LLM HTTP-call budget exhausted before provider={}", provider.provider());
                break;
            }
            try { T result = call.apply(provider); log.info("LLM provider succeeded: provider={}", provider.provider()); return result; }
            catch (LlmRateLimitException failure) {
                last = failure;
                rateLimits.recordRateLimit(provider.provider());
                log.warn("LLM provider rate-limited; failing over: provider={}, type={}, reason={}",
                        provider.provider(), failure.getClass().getSimpleName(), failure.getMessage());
                log.debug("LLM provider rate-limit stacktrace: provider={}", provider.provider(), failure);
            }
            catch (RuntimeException failure) {
                last = failure;
                log.warn("LLM provider failed; failing over: provider={}, type={}, reason={}, rootCause={}",
                        provider.provider(), failure.getClass().getSimpleName(), failure.getMessage(), rootCause(failure));
                log.debug("LLM provider failure stacktrace: provider={}", provider.provider(), failure);
            }
        }
        log.error("All eligible LLM providers failed: configuredProviders={}, lastFailureType={}, lastReason={}",
                providers.stream().map(provider -> provider.provider().name()).toList(),
                last == null ? "none" : last.getClass().getSimpleName(),
                last == null ? "none" : last.getMessage());
        if (last == null && !budget.hasRemaining()) {
            throw new LlmException("LLM HTTP-call budget exhausted before provider selection");
        }
        throw new LlmException("No LLM provider produced a valid IQL tool call", last);
    }

    private String rootCause(Throwable failure) {
        Throwable root = failure;
        while (root.getCause() != null) root = root.getCause();
        String message = root.getMessage();
        return root.getClass().getSimpleName() + (message == null ? "" : ": " + message);
    }
}
