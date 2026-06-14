package vdt.se.demo.adapter.out.elasticsearch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.adapter.config.AppProperties;

@Component
public class ElasticsearchHttpClient {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchHttpClient.class);

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final AppProperties properties;
    private final String elasticsearchUris;

    public ElasticsearchHttpClient(ObjectMapper objectMapper, RestTemplate restTemplate, AppProperties properties,
                                   @Value("${spring.elasticsearch.uris}") String elasticsearchUris) {
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.elasticsearchUris = elasticsearchUris;
    }

    public JsonNode search(JsonNode dsl) throws Exception {
        HttpHeaders headers = jsonHeaders();
        String response = restTemplate.postForObject(
                indexUrl(properties.getElasticsearch().getEventsIndex()) + "/_search",
                new HttpEntity<>(objectMapper.writeValueAsString(dsl), headers),
                String.class
        );
        return objectMapper.readTree(response);
    }

    public void ensureIndex(String indexName, String definition) {
        String url = indexUrl(indexName);
        try {
            restTemplate.getForEntity(url, String.class);
            return;
        } catch (HttpClientErrorException.NotFound ignored) {
            log.info("Creating Elasticsearch index: {}", indexName);
        }
        restTemplate.put(url, new HttpEntity<>(definition, jsonHeaders()));
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private String indexUrl(String indexName) {
        return elasticsearchBaseUrl() + "/" + indexName;
    }

    private String elasticsearchBaseUrl() {
        return elasticsearchUris.split(",")[0].trim().replaceAll("/+$", "");
    }
}
