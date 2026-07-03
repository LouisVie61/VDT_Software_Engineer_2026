package vdt.se.demo.application.port.outboundPort.cache;

import java.util.Optional;

public interface DslCachePort {
    Optional<String> findFinalizedDsl(String schemaVersion, String sessionId, String queryHash);

    void saveFinalizedDsl(String schemaVersion, String sessionId, String queryHash, String dsl);
}

