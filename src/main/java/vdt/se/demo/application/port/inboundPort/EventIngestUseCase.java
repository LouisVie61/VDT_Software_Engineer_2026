package vdt.se.demo.application.port.inboundPort;

import vdt.se.demo.application.dto.IngestFileCommand;
import vdt.se.demo.domain.model.IngestResult;

public interface EventIngestUseCase {
    IngestResult ingest(IngestFileCommand command);
}
