package vdt.se.demo.application.port.outboundPort.cache;

import tools.jackson.databind.JsonNode;
import java.util.Optional;

public interface BaseDslCachePort {
    Optional<JsonNode> findBaseDsl(String iqlCacheKey);
    void saveBaseDsl(String iqlCacheKey, JsonNode baseDsl);
}
