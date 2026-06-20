package vdt.se.demo.application.port.outboundPort.ingest;

import vdt.se.demo.application.dto.IngestFileCommand;
import vdt.se.demo.domain.valueObjects.EventFileFormat;

import java.util.UUID;

public interface EventSpoolPort {
    void spool(IngestFileCommand command, EventFileFormat format, UUID requestId);
}

