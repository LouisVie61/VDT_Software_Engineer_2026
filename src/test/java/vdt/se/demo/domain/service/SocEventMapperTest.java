package vdt.se.demo.domain.service;

import org.junit.jupiter.api.Test;
import vdt.se.demo.domain.model.SocEvent;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SocEventMapperTest {

    private final SocEventMapper mapper = new SocEventMapper();

    @Test
    void mapsRawFieldsToDomainEvent() {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("event_id", "1");
        fields.put("timestamp", "2025-05-28T23:46:49");
        fields.put("event_type", "auth");
        fields.put("src_ip", "10.0.0.1");
        fields.put("description", "Auth failed");
        fields.put("raw_log", "CEF raw");

        SocEvent event = mapper.fromJson(fields, "{\"event_id\":\"1\"}");

        assertThat(event.id()).isEqualTo("1");
        assertThat(event.eventType()).isEqualTo("auth");
        assertThat(event.ip()).isEqualTo("10.0.0.1");
        assertThat(event.message()).isEqualTo("Auth failed");
        assertThat(event.raw()).isEqualTo("CEF raw");
    }

    @Test
    void usesWholeJsonAsRawAndNormalizesInvalidIp() {
        SocEvent event = mapper.fromJson(Map.of(
                "event_id", "2",
                "description", "plain event",
                "src_ip", "N/A"
        ), "{\"event_id\":\"2\"}");

        assertThat(event.raw()).isEqualTo("{\"event_id\":\"2\"}");
        assertThat(event.ip()).isNull();
    }
}
