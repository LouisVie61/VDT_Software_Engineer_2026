package vdt.se.demo.adapter.out.ingest;

import org.junit.jupiter.api.Test;
import org.springframework.data.elasticsearch.BulkFailureException;
import vdt.se.demo.application.port.outboundPort.EventIndexPort;
import vdt.se.demo.domain.exception.BadQueryException;
import vdt.se.demo.domain.model.SocEvent;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventBatchBufferTest {

    @Test
    void includesFailedBulkDocumentDetailsInClientError() {
        Map<String, BulkFailureException.FailureDetails> failures = new LinkedHashMap<>();
        failures.put("event-1", new BulkFailureException.FailureDetails(
                400,
                "failed to parse field [metadata.advanced_metadata] of type [text]"
        ));
        EventBatchBuffer buffer = new EventBatchBuffer(new FailingIndexPort(
                new BulkFailureException("Bulk operation has failures", failures)
        ), 1);

        assertThatThrownBy(() -> buffer.add(event()))
                .isInstanceOf(BadQueryException.class)
                .hasMessageContaining("event-1")
                .hasMessageContaining("metadata.advanced_metadata");
    }

    private SocEvent event() {
        return new SocEvent("event-1", Instant.now(), "SIEM", "high", "auth", "alice", "host-1",
                "10.0.0.1", "10.0.0.1", null, "failed", "message", "raw", Map.of());
    }

    private record FailingIndexPort(RuntimeException exception) implements EventIndexPort {

        @Override
        public void ensureIndex() {
        }

        @Override
        public void indexBatch(List<SocEvent> events) {
            throw exception;
        }

        @Override
        public String indexName() {
            return "test-events";
        }
    }
}
