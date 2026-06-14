package vdt.se.demo.application.service;

import org.junit.jupiter.api.Test;
import vdt.se.demo.application.dto.IngestFileCommand;
import vdt.se.demo.application.port.outboundPort.EventIndexPort;
import vdt.se.demo.adapter.out.ingest.CsvEventFileReader;
import vdt.se.demo.adapter.out.ingest.CsvEventParser;
import vdt.se.demo.adapter.out.ingest.EventFileFormatDetector;
import vdt.se.demo.adapter.out.ingest.JsonlEventFileReader;
import vdt.se.demo.adapter.out.ingest.JsonlEventParser;
import vdt.se.demo.domain.exception.BadQueryException;
import vdt.se.demo.domain.model.IngestResult;
import vdt.se.demo.domain.model.SocEvent;
import vdt.se.demo.domain.service.SocEventMapper;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventIngestServiceTest {

    @Test
    void ingestsJsonlValidRowsAndCountsMalformedRows() {
        MemoryEventIndexPort index = new MemoryEventIndexPort();
        EventIngestService service = service(index, 1000);

        IngestResult result = service.ingest(command("events.jsonl", "application/json", """
                {"event_id":"1","timestamp":"2025-05-28T23:46:49","description":"ok"}
                not-json
                {"event_id":"2","timestamp":"2025-05-28T23:46:50","description":"ok2"}
                """));

        assertThat(result.totalRows()).isEqualTo(3);
        assertThat(result.indexedRows()).isEqualTo(2);
        assertThat(result.failedRows()).isEqualTo(1);
        assertThat(index.events).hasSize(2);
        assertThat(index.ensureCalls).isEqualTo(1);
    }

    @Test
    void ingestsCsvValidRowsAndKeepsExtraMetadata() {
        MemoryEventIndexPort index = new MemoryEventIndexPort();
        EventIngestService service = service(index, 1000);

        IngestResult result = service.ingest(command("events.csv", "text/csv", """
                event_id,timestamp,event_type,source,severity,user,src_ip,description,raw_log,custom_col
                1,2025-05-28T23:46:49,auth,SIEM,high,alice,10.0.0.1,"Auth failed, quoted","raw one",extra
                2,2025-05-28T23:46:50,network,SIEM,low,bob,10.0.0.2,Network event,raw two,extra2
                """));

        assertThat(result.totalRows()).isEqualTo(2);
        assertThat(result.indexedRows()).isEqualTo(2);
        assertThat(result.failedRows()).isZero();
        assertThat(index.events.getFirst().metadata()).containsEntry("custom_col", "extra");
    }

    @Test
    void csvMalformedRowsIncrementFailedRows() {
        MemoryEventIndexPort index = new MemoryEventIndexPort();
        EventIngestService service = service(index, 1000);

        IngestResult result = service.ingest(command("events.csv", "text/csv", """
                event_id,timestamp,description
                1,2025-05-28T23:46:49,ok
                2,2025-05-28T23:46:50
                """));

        assertThat(result.totalRows()).isEqualTo(2);
        assertThat(result.indexedRows()).isEqualTo(1);
        assertThat(result.failedRows()).isEqualTo(1);
    }

    @Test
    void flushesUsingConfiguredBatchSize() {
        MemoryEventIndexPort index = new MemoryEventIndexPort();
        EventIngestService service = service(index, 2);

        service.ingest(command("events.jsonl", "application/json", """
                {"event_id":"1"}
                {"event_id":"2"}
                {"event_id":"3"}
                """));

        assertThat(index.batchSizes).containsExactly(2, 1);
    }

    @Test
    void failsWholeRequestWhenIndexWriteFails() {
        MemoryEventIndexPort index = new MemoryEventIndexPort();
        index.failWrites = true;

        assertThatThrownBy(() -> service(index, 1000).ingest(command(
                "events.jsonl", "application/json", "{\"event_id\":\"1\"}")))
                .isInstanceOf(BadQueryException.class)
                .hasMessageContaining("Cannot write event batch");
    }

    @Test
    void rejectsUnsupportedEmptyAndNoRowFiles() {
        EventIngestService service = service(new MemoryEventIndexPort(), 1000);

        assertThatThrownBy(() -> service.ingest(command("events.txt", "text/plain", "x")))
                .isInstanceOf(BadQueryException.class)
                .hasMessageContaining("Unsupported event file format");
        assertThatThrownBy(() -> service.ingest(command("events.jsonl", "application/json", "")))
                .isInstanceOf(BadQueryException.class)
                .hasMessageContaining("empty");
        assertThatThrownBy(() -> service.ingest(command("events.csv", "text/csv", "event_id,timestamp\n")))
                .isInstanceOf(BadQueryException.class)
                .hasMessageContaining("no event rows");
    }

    private EventIngestService service(EventIndexPort indexPort, int batchSize) {
        SocEventMapper mapper = new SocEventMapper();
        return new EventIngestService(
                new EventFileFormatDetector(),
                new JsonlEventFileReader(new JsonlEventParser(new ObjectMapper(), mapper)),
                new CsvEventFileReader(new CsvEventParser(mapper)),
                indexPort,
                batchSize
        );
    }

    private IngestFileCommand command(String filename, String contentType, String content) {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        return new IngestFileCommand(filename, contentType, bytes.length, () -> new ByteArrayInputStream(bytes));
    }

    private static class MemoryEventIndexPort implements EventIndexPort {
        private final List<SocEvent> events = new ArrayList<>();
        private final List<Integer> batchSizes = new ArrayList<>();
        private int ensureCalls;
        private boolean failWrites;

        @Override
        public void ensureIndex() {
            ensureCalls++;
        }

        @Override
        public void indexBatch(List<SocEvent> events) {
            if (failWrites) {
                throw new IllegalStateException("index unavailable");
            }
            batchSizes.add(events.size());
            this.events.addAll(events);
        }

        @Override
        public String indexName() {
            return "test-events";
        }
    }
}
