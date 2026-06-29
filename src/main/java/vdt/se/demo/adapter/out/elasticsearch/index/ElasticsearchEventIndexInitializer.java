package vdt.se.demo.adapter.out.elasticsearch.index;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import vdt.se.demo.adapter.config.AppProperties;
import vdt.se.demo.adapter.out.elasticsearch.client.ElasticsearchHttpClient;

@Component
public class ElasticsearchEventIndexInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchEventIndexInitializer.class);

    private final ElasticsearchHttpClient httpClient;
    private final SocEventIndexDefinition indexDefinition;
    private final AppProperties properties;

    public ElasticsearchEventIndexInitializer(ElasticsearchHttpClient httpClient, SocEventIndexDefinition indexDefinition,
                                              AppProperties properties) {
        this.httpClient = httpClient;
        this.indexDefinition = indexDefinition;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.getElasticsearch().isInitializeIndex()) {
            return;
        }
        String indexName = properties.getElasticsearch().getEventsIndex();
        try {
            httpClient.ensureIndex(indexName, indexDefinition.json());
            httpClient.updateMapping(indexName, indexDefinition.mappingJson());
            log.debug("Elasticsearch event index is ready: {}", indexName);
        } catch (RuntimeException e) {
            log.warn("Elasticsearch event index initialization skipped for '{}': {}", indexName, e.getMessage());
        }
    }
}
