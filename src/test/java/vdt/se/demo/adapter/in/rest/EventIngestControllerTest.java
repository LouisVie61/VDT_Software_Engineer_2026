package vdt.se.demo.adapter.in.rest;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import vdt.se.demo.adapter.in.rest.dto.IngestResponse;
import vdt.se.demo.application.dto.IngestFileCommand;
import vdt.se.demo.application.port.inboundPort.EventIngestUseCase;
import vdt.se.demo.domain.model.IngestResult;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class EventIngestControllerTest {

    @Test
    void mapsMultipartFileToUseCaseAndKeepsResponseContract() {
        AtomicReference<IngestFileCommand> captured = new AtomicReference<>();
        EventIngestUseCase useCase = command -> {
            captured.set(command);
            return new IngestResult(3, 2, 1, "soc-events", 15);
        };
        EventIngestController controller = new EventIngestController(useCase);
        MockMultipartFile file = new MockMultipartFile("file", "events.jsonl", "application/json", "{}".getBytes());

        IngestResponse response = controller.importFile(file);

        assertThat(captured.get().filename()).isEqualTo("events.jsonl");
        assertThat(captured.get().contentType()).isEqualTo("application/json");
        assertThat(response).isEqualTo(new IngestResponse(3, 2, 1, "soc-events", 15));
    }
}
