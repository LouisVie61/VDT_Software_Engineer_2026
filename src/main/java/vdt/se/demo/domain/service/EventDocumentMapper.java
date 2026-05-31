package vdt.se.demo.domain.service;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import vdt.se.demo.adapter.out.elasticsearch.SocEventDocument;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class EventDocumentMapper {

    private static final Set<String> KNOWN_FIELDS = Set.of(
            "event_id", "timestamp", "source", "severity", "event_type", "user", "host", "hostname",
            "device_id", "ip", "src_ip", "dst_ip", "action", "message", "description", "raw", "raw_log"
    );

    public SocEventDocument fromJson(JsonNode node) {
        SocEventDocument document = new SocEventDocument();
        document.setId(text(node, "event_id", UUID.randomUUID().toString()));
        document.setTimestamp(parseTimestamp(text(node, "timestamp", null)));
        document.setSource(text(node, "source", null));
        document.setSeverity(text(node, "severity", null));
        document.setEventType(text(node, "event_type", null));
        document.setUser(text(node, "user", null));
        document.setHost(firstText(node, "host", "hostname", "device_id"));
        document.setSrcIp(text(node, "src_ip", null));
        document.setDstIp(text(node, "dst_ip", null));
        document.setIp(firstNonBlank(document.getSrcIp(), document.getDstIp(), text(node, "ip", null)));
        document.setAction(text(node, "action", null));
        document.setMessage(firstText(node, "description", "message", "raw_log"));
        document.setRaw(firstText(node, "raw_log", "raw"));
        if (document.getRaw() == null || document.getRaw().isBlank()) {
            document.setRaw(node.toString());
        }
        document.setMetadata(metadata(node));
        return document;
    }

    public SocEventDocument fromMap(Map<String, String> row) {
        SocEventDocument document = new SocEventDocument();
        document.setId(text(row, "event_id", UUID.randomUUID().toString()));
        document.setTimestamp(parseTimestamp(text(row, "timestamp", null)));
        document.setSource(text(row, "source", null));
        document.setSeverity(text(row, "severity", null));
        document.setEventType(text(row, "event_type", null));
        document.setUser(text(row, "user", null));
        document.setHost(firstText(row, "host", "hostname", "device_id"));
        document.setSrcIp(text(row, "src_ip", null));
        document.setDstIp(text(row, "dst_ip", null));
        document.setIp(firstNonBlank(document.getSrcIp(), document.getDstIp(), text(row, "ip", null)));
        document.setAction(text(row, "action", null));
        document.setMessage(firstText(row, "description", "message", "raw_log"));
        document.setRaw(firstText(row, "raw_log", "raw"));
        if (document.getRaw() == null || document.getRaw().isBlank()) {
            document.setRaw(row.toString());
        }
        document.setMetadata(metadata(row));
        return document;
    }

    public Map<String, Object> toMap(SocEventDocument document) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", document.getId());
        map.put("timestamp", document.getTimestamp() == null ? null : document.getTimestamp().toString());
        map.put("source", document.getSource());
        map.put("severity", document.getSeverity());
        map.put("event_type", document.getEventType());
        map.put("user", document.getUser());
        map.put("host", document.getHost());
        map.put("ip", document.getIp());
        map.put("src_ip", document.getSrcIp());
        map.put("dst_ip", document.getDstIp());
        map.put("action", document.getAction());
        map.put("message", document.getMessage());
        map.put("raw", document.getRaw());
        return map;
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

    private String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = text(node, field, null);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String firstText(Map<String, String> row, String... fields) {
        for (String field : fields) {
            String value = text(row, field, null);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank() && !"N/A".equalsIgnoreCase(value)) {
                return value;
            }
        }
        return null;
    }

    private String text(JsonNode node, String field, String defaultValue) {
        JsonNode value = node == null ? null : node.get(field);
        if (value == null || value.isNull()) {
            return defaultValue;
        }
        String text = value.asString();
        return text == null || text.isBlank() ? defaultValue : text;
    }

    private String text(Map<String, String> row, String field, String defaultValue) {
        String value = row == null ? null : row.get(field);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private Map<String, Object> metadata(JsonNode node) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (node != null && node.has("advanced_metadata")) {
            metadata.put("advanced_metadata", node.get("advanced_metadata").toString());
        }
        if (node != null && node.has("behavioral_analytics")) {
            metadata.put("behavioral_analytics", node.get("behavioral_analytics").toString());
        }
        return metadata;
    }

    private Map<String, Object> metadata(Map<String, String> row) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (row == null) {
            return metadata;
        }
        row.forEach((key, value) -> {
            if (!KNOWN_FIELDS.contains(key) && value != null && !value.isBlank()) {
                metadata.put(key, value);
            }
        });
        return metadata;
    }
}
