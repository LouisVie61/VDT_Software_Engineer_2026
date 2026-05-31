package vdt.se.demo.adapter.in.rest;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import vdt.se.demo.application.service.EventIngestService;
import vdt.se.demo.domain.model.IngestResult;

@RestController
@RequestMapping("/api/events")
public class EventIngestController {

    private final EventIngestService ingestService;

    public EventIngestController(EventIngestService ingestService) {
        this.ingestService = ingestService;
    }

    @PostMapping(path = "/import-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public IngestResult importFile(@RequestPart("file") MultipartFile file) {
        return ingestService.ingestFile(file);
    }
}
