package vdt.se.demo.application.service.ingest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vdt.se.demo.application.dto.IngestFileCommand;
import vdt.se.demo.application.port.inboundPort.EventIngestUseCase;
import vdt.se.demo.application.port.outboundPort.ingest.EventSpoolPort;
import vdt.se.demo.domain.exception.BadQueryException;
import vdt.se.demo.domain.model.IngestResult;
import vdt.se.demo.domain.model.SocEventSchema;
import vdt.se.demo.domain.valueObjects.EventFileFormat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

public class EventIngestService implements EventIngestUseCase {

    private static final Logger log = LoggerFactory.getLogger(EventIngestService.class);
    private static final String CANONICAL_CSV_HEADER = SocEventSchema.canonicalCsvHeader();

    private final EventSpoolPort eventSpoolPort;

    public EventIngestService(EventSpoolPort eventSpoolPort) {
        this.eventSpoolPort = eventSpoolPort;
    }

    @Override
    public IngestResult ingest(IngestFileCommand command) {
        validate(command);
        EventFileFormat format = detectFormat(command);
        if (format == EventFileFormat.CSV) {
            validateCanonicalCsvHeader(command);
        }

        UUID requestId = UUID.randomUUID();
        Instant submittedAt = Instant.now();
        eventSpoolPort.spool(command, format, requestId);
        log.debug("Accepted event ingest for Fluentd: requestId={}, filename={}, format={}, sizeBytes={}",
                requestId, command.filename(), format, command.size());
        return new IngestResult(requestId, command.filename(), format, command.size(), "ACCEPTED", submittedAt);
    }

    private void validate(IngestFileCommand command) {
        if (command == null || command.content() == null || command.size() == 0) {
            throw new BadQueryException("Uploaded event file is empty");
        }
    }

    private EventFileFormat detectFormat(IngestFileCommand command) {
        String filename = lower(command.filename());
        String contentType = lower(command.contentType());
        if (filename.endsWith(".jsonl") || "application/x-ndjson".equals(contentType)
                || "application/jsonl".equals(contentType)) {
            return EventFileFormat.JSONL;
        }
        if (filename.endsWith(".csv") || "text/csv".equals(contentType)) {
            return EventFileFormat.CSV;
        }
        throw new BadQueryException("Unsupported event file format. Upload JSONL or canonical CSV only.");
    }

    private void validateCanonicalCsvHeader(IngestFileCommand command) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(command.openStream(), StandardCharsets.UTF_8))) {
            String header = reader.readLine();
            if (header == null || header.isBlank()) {
                throw new BadQueryException("Uploaded CSV file is empty");
            }
            String normalized = header.strip().replace("\uFEFF", "");
            if (!CANONICAL_CSV_HEADER.equals(normalized)) {
                throw new BadQueryException("CSV header must be: " + CANONICAL_CSV_HEADER);
            }
        } catch (BadQueryException e) {
            throw e;
        } catch (Exception e) {
            throw new BadQueryException("Cannot read uploaded CSV header", e);
        }
    }

    private String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}

