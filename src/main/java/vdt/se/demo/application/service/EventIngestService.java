package vdt.se.demo.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vdt.se.demo.application.dto.IngestFileCommand;
import vdt.se.demo.application.port.inboundPort.EventIngestUseCase;
import vdt.se.demo.application.port.outboundPort.EventIndexPort;
import vdt.se.demo.adapter.out.ingest.CsvEventFileReader;
import vdt.se.demo.adapter.out.ingest.EventBatchBuffer;
import vdt.se.demo.adapter.out.ingest.EventFileFormat;
import vdt.se.demo.adapter.out.ingest.EventFileFormatDetector;
import vdt.se.demo.adapter.out.ingest.EventFileReader;
import vdt.se.demo.adapter.out.ingest.EventParseResult;
import vdt.se.demo.adapter.out.ingest.JsonlEventFileReader;
import vdt.se.demo.domain.exception.BadQueryException;
import vdt.se.demo.domain.model.IngestResult;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

public class EventIngestService implements EventIngestUseCase {

    private static final Logger log = LoggerFactory.getLogger(EventIngestService.class);

    private final EventFileFormatDetector formatDetector;
    private final JsonlEventFileReader jsonlReader;
    private final CsvEventFileReader csvReader;
    private final EventIndexPort eventIndexPort;
    private final int batchSize;

    public EventIngestService(EventFileFormatDetector formatDetector, JsonlEventFileReader jsonlReader,
                              CsvEventFileReader csvReader, EventIndexPort eventIndexPort, int batchSize) {
        this.formatDetector = formatDetector;
        this.jsonlReader = jsonlReader;
        this.csvReader = csvReader;
        this.eventIndexPort = eventIndexPort;
        this.batchSize = Math.max(1, batchSize);
    }

    @Override
    public IngestResult ingest(IngestFileCommand command) {
        validate(command);
        EventFileFormat format = formatDetector.detect(command);
        Instant started = Instant.now();
        log.info("Starting event ingest: filename={}, format={}, sizeBytes={}, batchSize={}, index={}",
                command.filename(), format, command.size(), batchSize, eventIndexPort.indexName());

        ensureIndex();
        EventBatchBuffer batch = new EventBatchBuffer(eventIndexPort, batchSize);
        EventParseResult parsed = read(command, reader(format), batch);
        batch.finish();
        if (parsed.totalRows() == 0) {
            throw new BadQueryException("Uploaded event file has no event rows");
        }

        long durationMs = Duration.between(started, Instant.now()).toMillis();
        log.info("Completed event ingest: totalRows={}, indexedRows={}, failedRows={}, durationMs={}, index={}",
                parsed.totalRows(), batch.indexedRows(), parsed.failedRows(), durationMs, eventIndexPort.indexName());
        return new IngestResult(
                parsed.totalRows(),
                batch.indexedRows(),
                parsed.failedRows(),
                eventIndexPort.indexName(),
                durationMs
        );
    }

    private void validate(IngestFileCommand command) {
        if (command == null || command.content() == null || command.size() == 0) {
            throw new BadQueryException("Uploaded event file is empty");
        }
    }

    private EventFileReader reader(EventFileFormat format) {
        return switch (format) {
            case JSONL -> jsonlReader;
            case CSV -> csvReader;
        };
    }

    private EventParseResult read(IngestFileCommand command, EventFileReader fileReader, EventBatchBuffer batch) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(command.openStream(), StandardCharsets.UTF_8))) {
            return fileReader.read(reader, batch::add);
        } catch (BadQueryException e) {
            throw e;
        } catch (Exception e) {
            throw new BadQueryException("Cannot read uploaded event file", e);
        }
    }

    private void ensureIndex() {
        try {
            eventIndexPort.ensureIndex();
        } catch (Exception e) {
            throw new BadQueryException("Cannot prepare Elasticsearch index: " + eventIndexPort.indexName(), e);
        }
    }
}
