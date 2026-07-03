package vdt.se.demo.application.service.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vdt.se.demo.application.port.outboundPort.cache.IntentCachePort;
import vdt.se.demo.domain.model.CachedIntent;
import vdt.se.demo.domain.model.RoutingHint;
import vdt.se.demo.domain.model.SearchIntent;

import java.util.Optional;

public class ContextRetrievalService {
    private static final Logger log = LoggerFactory.getLogger(ContextRetrievalService.class);

    private final IntentCachePort intentCachePort;

    public ContextRetrievalService(IntentCachePort intentCachePort) {
        this.intentCachePort = intentCachePort;
    }

    public Optional<SearchIntent> inheritedIntent(String sessionId, String schemaVersion, String question,
                                                  RoutingHint heuristic, RoutingHint semantic) {
        if (!isShort(question) || !looksLikeFollowUp(question) || heuristic == null || semantic == null
                || !heuristic.lowConfidence() || !semantic.lowConfidence()) {
            return Optional.empty();
        }
        Optional<CachedIntent> cachedIntent = intentCachePort.findLastClassifiedIntent(sessionId)
                .filter(cached -> schemaVersion.equals(cached.schemaVersion()));
        cachedIntent.ifPresent(cached -> log.info(
                "Last classified intent cache hit: sessionId={}, schemaVersion={}, intent={}",
                sessionId, schemaVersion, cached.intent().getIntent()));
        return cachedIntent.map(CachedIntent::intent);
    }

    private boolean isShort(String question) {
        if (question == null || question.isBlank()) {
            return false;
        }
        return question.strip().split("\\s+").length <= 4;
    }

    private boolean looksLikeFollowUp(String question) {
        String text = question == null ? "" : question.strip().toLowerCase(java.util.Locale.ROOT);
        return text.matches(".*\\b(them|more|next|previous|prev|continue|tiep|nua|khac|same|filter|loc)\\b.*");
    }
}

