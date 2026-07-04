package vdt.se.demo.adapter.out.llm;

import vdt.se.demo.domain.valueObjects.LlmProvider;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.EnumMap;
import java.util.Map;

final class RateLimitCircuitBreaker {
    private final int threshold;
    private final Duration window;
    private final Duration cooldown;
    private final Clock clock;
    private final Map<LlmProvider, ProviderState> states = new EnumMap<>(LlmProvider.class);

    RateLimitCircuitBreaker(int threshold, Duration window, Duration cooldown, Clock clock) {
        if (threshold < 1 || window.isZero() || window.isNegative() || cooldown.isZero() || cooldown.isNegative())
            throw new IllegalArgumentException("Circuit-breaker settings must be positive");
        this.threshold = threshold;
        this.window = window;
        this.cooldown = cooldown;
        this.clock = clock;
    }

    synchronized boolean permits(LlmProvider provider) {
        ProviderState state = states.get(provider);
        return state == null || state.openUntil == null || !clock.instant().isBefore(state.openUntil);
    }

    synchronized void recordRateLimit(LlmProvider provider) {
        Instant now = clock.instant();
        ProviderState state = states.computeIfAbsent(provider, ignored -> new ProviderState());
        state.failures.removeIf(failure -> failure.isBefore(now.minus(window)));
        state.failures.addLast(now);
        if (state.failures.size() >= threshold) {
            state.openUntil = now.plus(cooldown);
            state.failures.clear();
        }
    }

    private static final class ProviderState {
        private final ArrayDeque<Instant> failures = new ArrayDeque<>();
        private Instant openUntil;
    }
}
