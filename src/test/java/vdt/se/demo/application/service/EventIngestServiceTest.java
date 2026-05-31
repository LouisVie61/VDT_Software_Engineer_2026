package vdt.se.demo.application.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.adapter.config.AppProperties;
import vdt.se.demo.adapter.out.elasticsearch.SocEventRepository;
import vdt.se.demo.domain.exception.BadQueryException;
import vdt.se.demo.domain.model.IngestResult;
import vdt.se.demo.domain.service.EventDocumentMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EventIngestServiceTest {

    @Test
    void ingestsJsonlValidRowsAndCountsMalformedRows() {
        EventIngestService service = service();
        MockMultipartFile file = new MockMultipartFile("file", "events.jsonl", "application/json",
                """
                {"event_id":"1","timestamp":"2025-05-28T23:46:49","description":"ok"}
                not-json
                {"event_id":"2","timestamp":"2025-05-28T23:46:50","description":"ok2"}
                """.getBytes());

        IngestResult result = service.ingestFile(file);

        assertThat(result.totalRows()).isEqualTo(3);
        assertThat(result.indexedRows()).isEqualTo(2);
        assertThat(result.failedRows()).isEqualTo(1);
    }

    @Test
    void ingestsCsvValidRows() {
        EventIngestService service = service();
        MockMultipartFile file = new MockMultipartFile("file", "events.csv", "text/csv",
                """
                event_id,timestamp,event_type,source,severity,user,src_ip,description,raw_log,custom_col
                1,2025-05-28T23:46:49,auth,SIEM,high,alice,10.0.0.1,"Auth failed, quoted","raw one",extra
                2,2025-05-28T23:46:50,network,SIEM,low,bob,10.0.0.2,Network event,raw two,extra2
                """.getBytes());

        IngestResult result = service.ingestFile(file);

        assertThat(result.totalRows()).isEqualTo(2);
        assertThat(result.indexedRows()).isEqualTo(2);
        assertThat(result.failedRows()).isZero();
    }

    @Test
    void csvMalformedRowsIncrementFailedRows() {
        EventIngestService service = service();
        MockMultipartFile file = new MockMultipartFile("file", "events.csv", "text/csv",
                """
                event_id,timestamp,description
                1,2025-05-28T23:46:49,ok
                2,2025-05-28T23:46:50
                """.getBytes());

        IngestResult result = service.ingestFile(file);

        assertThat(result.totalRows()).isEqualTo(2);
        assertThat(result.indexedRows()).isEqualTo(1);
        assertThat(result.failedRows()).isEqualTo(1);
    }

    @Test
    void rejectsUnsupportedFileFormat() {
        EventIngestService service = service();

        assertThatThrownBy(() -> service.ingestFile(new MockMultipartFile(
                "file", "events.txt", "text/plain", "x".getBytes())))
                .isInstanceOf(BadQueryException.class)
                .hasMessageContaining("Unsupported event file format");
    }

    @Test
    void rejectsEmptyFile() {
        EventIngestService service = service();

        assertThatThrownBy(() -> service.ingestFile(new MockMultipartFile("file", new byte[0])))
                .isInstanceOf(BadQueryException.class)
                .hasMessageContaining("empty");
    }

    private EventIngestService service() {
        SocEventRepository repository = mock(SocEventRepository.class);
        when(repository.saveAll(any(Iterable.class))).thenAnswer(invocation -> invocation.getArgument(0));
        return new EventIngestService(new ObjectMapper(), new EventDocumentMapper(), repository, new AppProperties());
    }
}
