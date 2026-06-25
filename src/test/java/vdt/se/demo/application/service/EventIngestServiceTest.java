package vdt.se.demo.application.service;

import org.junit.jupiter.api.Test;
import vdt.se.demo.application.dto.IngestFileCommand;
import vdt.se.demo.application.port.outboundPort.ingest.EventSpoolPort;
import vdt.se.demo.application.service.ingest.EventIngestService;
import vdt.se.demo.domain.exception.BadQueryException;
import vdt.se.demo.domain.model.IngestResult;
import vdt.se.demo.domain.valueObjects.EventFileFormat;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventIngestServiceTest {

    @Test
    void acceptsJsonlAndSpoolsToFluentd() {
        CapturingSpoolPort spool = new CapturingSpoolPort();
        EventIngestService service = new EventIngestService(spool);

        IngestResult result = service.ingest(command("events.jsonl", "application/x-ndjson", """
                {"event_id":"1","timestamp":"2025-05-28T23:46:49","description":"ok"}
                {"event_id":"2","timestamp":"2025-05-28T23:46:50","description":"ok2"}
                """));

        assertThat(result.requestId()).isNotNull();
        assertThat(result.fileName()).isEqualTo("events.jsonl");
        assertThat(result.format()).isEqualTo(EventFileFormat.JSONL);
        assertThat(result.status()).isEqualTo("ACCEPTED");
        assertThat(spool.format).isEqualTo(EventFileFormat.JSONL);
        assertThat(spool.requestId).isEqualTo(result.requestId());
    }

    @Test
    void acceptsCanonicalCsvHeader() {
        CapturingSpoolPort spool = new CapturingSpoolPort();
        EventIngestService service = new EventIngestService(spool);

        IngestResult result = service.ingest(command("events.csv", "text/csv", """
                timestamp,source,severity,event_type,user,host,ip,message,raw
                2025-05-28T23:46:49Z,SIEM,high,auth,alice,host-1,10.0.0.1,Auth failed,raw one
                """));

        assertThat(result.format()).isEqualTo(EventFileFormat.CSV);
        assertThat(spool.format).isEqualTo(EventFileFormat.CSV);
    }

    @Test
    void rejectsUnsupportedEmptyAndNonCanonicalCsvFiles() {
        EventIngestService service = new EventIngestService(new CapturingSpoolPort());

        assertThatThrownBy(() -> service.ingest(command("events.txt", "text/plain", "x")))
                .isInstanceOf(BadQueryException.class)
                .hasMessageContaining("Unsupported event file format");
        assertThatThrownBy(() -> service.ingest(command("events.jsonl", "application/x-ndjson", "")))
                .isInstanceOf(BadQueryException.class)
                .hasMessageContaining("empty");
        assertThatThrownBy(() -> service.ingest(command("events.csv", "text/csv", "event_id,timestamp\n")))
                .isInstanceOf(BadQueryException.class)
                .hasMessageContaining("CSV header must be");
    }

    private IngestFileCommand command(String filename, String contentType, String content) {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        return new IngestFileCommand(filename, contentType, bytes.length, () -> new ByteArrayInputStream(bytes));
    }

    private static class CapturingSpoolPort implements EventSpoolPort {
        private EventFileFormat format;
        private UUID requestId;

        @Override
        public void spool(IngestFileCommand command, EventFileFormat format, UUID requestId) {
            this.format = format;
            this.requestId = requestId;
        }
    }
}
