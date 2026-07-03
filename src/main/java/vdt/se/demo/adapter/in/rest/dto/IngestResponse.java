package vdt.se.demo.adapter.in.rest.dto;

import vdt.se.demo.domain.model.IngestResult;

import java.time.Instant;
import java.util.UUID;

public record IngestResponse(
        UUID requestId,
        String fileName,
        String format,
        long bytesAccepted,
        String status,
        Instant submittedAt
) {
    public static IngestResponse from(IngestResult result) {
        return new IngestResponse(
                result.requestId(),
                result.fileName(),
                result.format().directoryName(),
                result.bytesAccepted(),
                result.status(),
                result.submittedAt()
        );
    }
}
