package vdt.se.demo.domain.model;

import vdt.se.demo.domain.valueObjects.EventFileFormat;

import java.time.Instant;
import java.util.UUID;

public record IngestResult(
        UUID requestId,
        String fileName,
        EventFileFormat format,
        long bytesAccepted,
        String status,
        Instant submittedAt
) {
}
