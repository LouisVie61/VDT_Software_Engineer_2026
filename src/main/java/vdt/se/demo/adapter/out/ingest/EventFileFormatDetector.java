package vdt.se.demo.adapter.out.ingest;

import vdt.se.demo.application.dto.IngestFileCommand;
import vdt.se.demo.domain.exception.BadQueryException;

import java.util.Locale;

public class EventFileFormatDetector {

    public EventFileFormat detect(IngestFileCommand command) {
        String normalizedName = normalize(command.filename());
        if (normalizedName.endsWith(".jsonl")) {
            return EventFileFormat.JSONL;
        }
        if (normalizedName.endsWith(".csv")) {
            return EventFileFormat.CSV;
        }

        String normalizedContentType = normalize(command.contentType());
        if (normalizedContentType.contains("csv")) {
            return EventFileFormat.CSV;
        }
        if (normalizedContentType.contains("json")) {
            return EventFileFormat.JSONL;
        }

        throw new BadQueryException("Unsupported event file format. Supported formats: .jsonl, .csv");
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
