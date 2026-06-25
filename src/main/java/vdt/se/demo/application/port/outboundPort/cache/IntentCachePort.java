package vdt.se.demo.application.port.outboundPort.cache;

import vdt.se.demo.domain.model.CachedIntent;
import vdt.se.demo.domain.model.PendingConfirmation;
import vdt.se.demo.domain.model.SearchIntent;

import java.util.Optional;

public interface IntentCachePort {
    Optional<CachedIntent> findLastClassifiedIntent(String sessionId);

    void saveLastClassifiedIntent(String sessionId, String schemaVersion, SearchIntent intent);

    void savePendingConfirmation(PendingConfirmation confirmation);

    Optional<PendingConfirmation> findPendingConfirmation(String confirmationId);
}

