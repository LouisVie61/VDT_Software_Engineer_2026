package vdt.se.demo.domain.service;

import vdt.se.demo.domain.model.SocEvent;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class SocEventMapper {

    private static final Set<String> KNOWN_FIELDS = Set.of(
            "event_id", "timestamp", "source", "severity", "event_type", "user", "host", "hostname",
            "device_id", "ip", "src_ip", "dst_ip", "action", "message", "description", "raw", "raw_log"
    );

    public SocEvent fromJson(Map<String, Object> fields, String rawJson) {
        return map(fields, rawJson, false);
    }

    public SocEvent fromCsv(Map<String, String> fields) {
        return map(fields, fields.toString(), true);
    }

    private SocEvent map(Map<String, ?> fields, String rawFallback, boolean includeExtraMetadata) {
        String srcIp = normalizeIp(text(fields, "src_ip"));
        String dstIp = normalizeIp(text(fields, "dst_ip"));
        String ip = firstNonBlank(srcIp, dstIp, normalizeIp(text(fields, "ip")));
        String raw = firstText(fields, "raw_log", "raw");

        return new SocEvent(
                value(text(fields, "event_id"), UUID.randomUUID().toString()),
                parseTimestamp(text(fields, "timestamp")),
                text(fields, "source"),
                text(fields, "severity"),
                text(fields, "event_type"),
                text(fields, "user"),
                firstText(fields, "host", "hostname", "device_id"),
                ip,
                srcIp,
                dstIp,
                text(fields, "action"),
                firstText(fields, "description", "message", "raw_log"),
                value(raw, rawFallback),
                metadata(fields, includeExtraMetadata)
        );
    }

    private Instant parseTimestamp(String value) {
        if (value == null || value.isBlank()) {
            return Instant.now();
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ignored) {
            return LocalDateTime.parse(value).toInstant(ZoneOffset.UTC);
        }
    }

    private String firstText(Map<String, ?> fields, String... names) {
        for (String name : names) {
            String value = text(fields, name);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String text(Map<String, ?> fields, String name) {
        Object value = fields == null ? null : fields.get(name);
        if (value == null) {
            return null;
        }
        String text = value.toString();
        return text.isBlank() ? null : text;
    }

    private String normalizeIp(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if ("N/A".equalsIgnoreCase(normalized) || "UNKNOWN".equalsIgnoreCase(normalized)) {
            return null;
        }
        return normalized;
    }

    private Map<String, Object> metadata(Map<String, ?> fields, boolean includeExtraMetadata) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (fields == null) {
            return metadata;
        }
        fields.forEach((key, value) -> {
            boolean selectedJsonMetadata = "advanced_metadata".equals(key) || "behavioral_analytics".equals(key);
            if ((includeExtraMetadata && !KNOWN_FIELDS.contains(key) || selectedJsonMetadata) && value != null) {
                metadata.put(key, value);
            }
        });
        return metadata;
    }

    private String value(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
