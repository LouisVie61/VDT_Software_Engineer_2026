package vdt.se.demo.application.service.context;

import org.junit.jupiter.api.Test;
import vdt.se.demo.application.port.outboundPort.cache.IntentCachePort;
import vdt.se.demo.domain.model.CachedIntent;
import vdt.se.demo.domain.model.PendingConfirmation;
import vdt.se.demo.domain.model.RoutingHint;
import vdt.se.demo.domain.model.SearchIntent;
import vdt.se.demo.domain.valueObjects.TemplateType;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ContextRetrievalServiceTest {

    @Test
    void doesNotInheritIntentForUnrelatedShortQuery() {
        ContextRetrievalService service = new ContextRetrievalService(new StubIntentCachePort());

        Optional<SearchIntent> inherited = service.inheritedIntent(
                "session-a", "v1", "high",
                lowConfidenceHint(), lowConfidenceHint());

        assertThat(inherited).isEmpty();
    }

    @Test
    void inheritsIntentForShortFollowUpQuery() {
        ContextRetrievalService service = new ContextRetrievalService(new StubIntentCachePort());

        Optional<SearchIntent> inherited = service.inheritedIntent(
                "session-a", "v1", "loc tiep",
                lowConfidenceHint(), lowConfidenceHint());

        assertThat(inherited).isPresent();
    }

    private RoutingHint lowConfidenceHint() {
        return RoutingHint.builder()
                .templateType(TemplateType.SIMPLE_SEARCH)
                .confidence(0.30d)
                .build();
    }

    private static class StubIntentCachePort implements IntentCachePort {
        @Override
        public Optional<CachedIntent> findLastClassifiedIntent(String sessionId) {
            return Optional.of(CachedIntent.builder()
                    .schemaVersion("v1")
                    .intent(SearchIntent.builder().textQuery("previous").build())
                    .build());
        }

        @Override
        public void saveLastClassifiedIntent(String sessionId, String schemaVersion, SearchIntent intent) {
        }

        @Override
        public void savePendingConfirmation(PendingConfirmation confirmation) {
        }

        @Override
        public Optional<PendingConfirmation> findPendingConfirmation(String confirmationId) {
            return Optional.empty();
        }
    }
}
