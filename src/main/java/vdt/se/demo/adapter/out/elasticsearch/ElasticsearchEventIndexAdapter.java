package vdt.se.demo.adapter.out.elasticsearch;

import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Component;
import vdt.se.demo.adapter.config.AppProperties;
import vdt.se.demo.application.port.outboundPort.EventIndexPort;
import vdt.se.demo.domain.model.SocEvent;

import java.util.List;

@Component
public class ElasticsearchEventIndexAdapter implements EventIndexPort {

    private final ElasticsearchOperations operations;
    private final ElasticsearchHttpClient httpClient;
    private final SocEventDocumentMapper documentMapper;
    private final SocEventIndexDefinition indexDefinition;
    private final AppProperties properties;

    public ElasticsearchEventIndexAdapter(ElasticsearchOperations operations, ElasticsearchHttpClient httpClient,
                                          SocEventDocumentMapper documentMapper,
                                          SocEventIndexDefinition indexDefinition, AppProperties properties) {
        this.operations = operations;
        this.httpClient = httpClient;
        this.documentMapper = documentMapper;
        this.indexDefinition = indexDefinition;
        this.properties = properties;
    }

    @Override
    public void ensureIndex() {
        httpClient.ensureIndex(indexName(), indexDefinition.json());
    }

    @Override
    public void indexBatch(List<SocEvent> events) {
        List<SocEventDocument> documents = events.stream().map(documentMapper::toDocument).toList();
        operations.save(documents, IndexCoordinates.of(indexName()));
    }

    @Override
    public String indexName() {
        return properties.getElasticsearch().getEventsIndex();
    }
}
