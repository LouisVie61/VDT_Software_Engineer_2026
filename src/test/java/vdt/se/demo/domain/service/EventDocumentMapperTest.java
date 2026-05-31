package vdt.se.demo.domain.service;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.adapter.out.elasticsearch.SocEventDocument;

import static org.assertj.core.api.Assertions.assertThat;

class EventDocumentMapperTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final EventDocumentMapper mapper = new EventDocumentMapper();

    @Test
    void mapsDatasetJsonToDocument() throws Exception {
        JsonNode node = objectMapper.readTree("""
                {"event_id":"1","timestamp":"2025-05-28T23:46:49","event_type":"auth","source":"SIEM",
                 "severity":"high","user":"alice","src_ip":"10.0.0.1","description":"Auth failed",
                 "raw_log":"CEF raw","action":"failed"}
                """);

        SocEventDocument document = mapper.fromJson(node);

        assertThat(document.getId()).isEqualTo("1");
        assertThat(document.getEventType()).isEqualTo("auth");
        assertThat(document.getIp()).isEqualTo("10.0.0.1");
        assertThat(document.getMessage()).isEqualTo("Auth failed");
        assertThat(document.getRaw()).isEqualTo("CEF raw");
    }

    @Test
    void usesWholeJsonAsRawWhenRawLogMissing() throws Exception {
        JsonNode node = objectMapper.readTree("""
                {"event_id":"2","timestamp":"2025-05-28T23:46:49","description":"plain event"}
                """);

        SocEventDocument document = mapper.fromJson(node);

        assertThat(document.getRaw()).contains("\"event_id\":\"2\"");
        assertThat(mapper.toMap(document)).containsEntry("message", "plain event");
    }
}
