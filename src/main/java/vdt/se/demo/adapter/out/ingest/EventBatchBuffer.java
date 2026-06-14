package vdt.se.demo.adapter.out.ingest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vdt.se.demo.application.port.outboundPort.EventIndexPort;
import vdt.se.demo.domain.exception.BadQueryException;
import vdt.se.demo.domain.model.SocEvent;

import java.util.ArrayList;
import java.util.List;

public class EventBatchBuffer {

    private static final Logger log = LoggerFactory.getLogger(EventBatchBuffer.class);
    private static final long PROGRESS_LOG_INTERVAL = 10_000;

    private final EventIndexPort eventIndexPort;
    private final int batchSize;
    private final List<SocEvent> batch = new ArrayList<>();
    private long indexedRows;

    public EventBatchBuffer(EventIndexPort eventIndexPort, int batchSize) {
        this.eventIndexPort = eventIndexPort;
        this.batchSize = Math.max(1, batchSize);
    }

    public void add(SocEvent event) {
        batch.add(event);
        if (batch.size() >= batchSize) {
            flush();
        }
    }

    public void finish() {
        flush();
    }

    public long indexedRows() {
        return indexedRows;
    }

    private void flush() {
        if (batch.isEmpty()) {
            return;
        }
        int size = batch.size();
        try {
            eventIndexPort.indexBatch(List.copyOf(batch));
        } catch (Exception e) {
            log.error("Cannot write event batch to Elasticsearch: batchSize={}, indexedRows={}, index={}",
                    size, indexedRows, eventIndexPort.indexName(), e);
            throw new BadQueryException("Cannot write event batch to Elasticsearch: " + rootMessage(e), e);
        }
        batch.clear();
        indexedRows += size;
        if (indexedRows % PROGRESS_LOG_INTERVAL == 0) {
            log.info("Event ingest progress: indexedRows={}, index={}", indexedRows, eventIndexPort.indexName());
        }
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? throwable.getMessage() : current.getMessage();
    }
}
