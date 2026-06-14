package vdt.se.demo.adapter.in.rest;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import vdt.se.demo.adapter.in.rest.dto.IngestResponse;
import vdt.se.demo.application.dto.IngestFileCommand;
import vdt.se.demo.application.port.inboundPort.EventIngestUseCase;

@RestController
@RequestMapping("/api/events")
public class EventIngestController {

    private final EventIngestUseCase ingestUseCase;

    public EventIngestController(EventIngestUseCase ingestUseCase) {
        this.ingestUseCase = ingestUseCase;
    }

    @PostMapping(path = "/import-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public IngestResponse importFile(@RequestPart("file") MultipartFile file) {
        IngestFileCommand command = new IngestFileCommand(
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                file::getInputStream
        );
        return IngestResponse.from(ingestUseCase.ingest(command));
    }
}
