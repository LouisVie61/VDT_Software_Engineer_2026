package vdt.se.demo.adapter.out.elasticsearch;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import vdt.se.demo.adapter.config.AppProperties;
import vdt.se.demo.domain.model.SocEvent;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ElasticsearchEventIndexAdapterTest {

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void writesBatchToConfiguredIndex() {
        ElasticsearchOperations operations = mock(ElasticsearchOperations.class);
        AppProperties properties = new AppProperties();
        properties.getElasticsearch().setEventsIndex("custom-events");
        ElasticsearchEventIndexAdapter adapter = new ElasticsearchEventIndexAdapter(
                operations,
                mock(ElasticsearchHttpClient.class),
                new SocEventDocumentMapper(),
                new SocEventIndexDefinition(),
                properties
        );
        SocEvent event = new SocEvent("1", Instant.now(), "SIEM", "high", "auth", "alice", "host-1",
                "10.0.0.1", "10.0.0.1", null, "failed", "message", "raw", Map.of());

        adapter.indexBatch(List.of(event));

        ArgumentCaptor<IndexCoordinates> coordinates = ArgumentCaptor.forClass(IndexCoordinates.class);
        verify(operations).save(any(Iterable.class), coordinates.capture());
        assertThat(coordinates.getValue().getIndexName()).isEqualTo("custom-events");
    }
}
