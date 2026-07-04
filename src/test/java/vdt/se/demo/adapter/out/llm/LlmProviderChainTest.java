package vdt.se.demo.adapter.out.llm;

import org.junit.jupiter.api.Test;
import vdt.se.demo.adapter.config.AppProperties;
import vdt.se.demo.application.port.outboundPort.llm.LlmProviderPort;
import vdt.se.demo.application.port.outboundPort.llm.LlmCallBudget;
import vdt.se.demo.domain.exception.LlmException;
import vdt.se.demo.domain.exception.LlmRateLimitException;
import vdt.se.demo.domain.valueObjects.LlmProvider;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LlmProviderChainTest {
    @Test
    void outboundCallBudgetCapsProviderFailoverAttempts() {
        AppProperties properties = new AppProperties();
        properties.getLlm().setProviderOrder("GPT,GEMINI,GROQ");
        List<LlmProvider> calls = new ArrayList<>();
        var chain = new LlmProviderChain(List.of(
                provider(LlmProvider.GEMINI), provider(LlmProvider.GROQ), provider(LlmProvider.GPT)), properties);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> chain.firstSuccessful(provider -> {
                    calls.add(provider.provider());
                    throw new LlmException("invalid tool call");
                }, new LlmCallBudget(1)))
                .isInstanceOf(LlmException.class);

        assertThat(calls).containsExactly(LlmProvider.GPT);
    }

    @Test
    void fallsBackToOpenRouterLast() {
        AppProperties properties = new AppProperties();
        properties.getLlm().setProviderOrder("GPT,GEMINI,GROQ,OPENROUTER");
        List<LlmProvider> calls = new ArrayList<>();
        var chain = new LlmProviderChain(List.of(
                provider(LlmProvider.OPENROUTER), provider(LlmProvider.GROQ),
                provider(LlmProvider.GEMINI), provider(LlmProvider.GPT)), properties);

        String result = chain.firstSuccessful(provider -> {
            calls.add(provider.provider());
            if (provider.provider() != LlmProvider.OPENROUTER) throw new LlmException("unavailable");
            return "openrouter";
        });

        assertThat(result).isEqualTo("openrouter");
        assertThat(calls).containsExactly(LlmProvider.GPT, LlmProvider.GEMINI, LlmProvider.GROQ, LlmProvider.OPENROUTER);
    }

    @Test
    void opensCircuitAfterRateLimitThresholdAndUsesFallbackUntilCooldownExpires() {
        MutableClock clock = new MutableClock();
        var breaker = new RateLimitCircuitBreaker(2, Duration.ofSeconds(60), Duration.ofSeconds(30), clock);
        List<LlmProvider> calls = new ArrayList<>();
        LlmProviderPort primary = provider(LlmProvider.GEMINI);
        LlmProviderPort fallback = provider(LlmProvider.GROQ);
        var chain = new LlmProviderChain(List.of(primary, fallback), new AppProperties(), breaker);

        String first = chain.firstSuccessful(provider -> invoke(provider, calls));
        String second = chain.firstSuccessful(provider -> invoke(provider, calls));
        String third = chain.firstSuccessful(provider -> invoke(provider, calls));
        assertThat(List.of(first, second, third)).containsOnly("fallback");
        assertThat(calls).containsExactly(LlmProvider.GEMINI, LlmProvider.GROQ,
                LlmProvider.GEMINI, LlmProvider.GROQ, LlmProvider.GROQ);

        clock.advance(Duration.ofSeconds(31));
        String afterCooldown = chain.firstSuccessful(provider -> invoke(provider, calls));
        assertThat(afterCooldown).isEqualTo("fallback");
        assertThat(calls.get(calls.size() - 2)).isEqualTo(LlmProvider.GEMINI);
    }

    private String invoke(LlmProviderPort provider, List<LlmProvider> calls) {
        calls.add(provider.provider());
        if (provider.provider() == LlmProvider.GEMINI) throw new LlmRateLimitException("429");
        return "fallback";
    }

    private LlmProviderPort provider(LlmProvider type) {
        return new LlmProviderPort() {
            public LlmProvider provider() { return type; }
            public String complete(String prompt) { return "unused"; }
        };
    }

    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-07-04T00:00:00Z");
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return now; }
        void advance(Duration duration) { now = now.plus(duration); }
    }
}
