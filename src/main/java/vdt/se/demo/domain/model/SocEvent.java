package vdt.se.demo.domain.model;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public record SocEvent(
        String id,
        Instant timestamp,
        String source,
        String severity,
        String eventType,
        String user,
        String host,
        String ip,
        String srcIp,
        String dstIp,
        String action,
        String message,
        String raw,
        Map<String, Object> metadata
) {
    public SocEvent {
        metadata = metadata == null ? Map.of() : new LinkedHashMap<>(metadata);
    }
}
