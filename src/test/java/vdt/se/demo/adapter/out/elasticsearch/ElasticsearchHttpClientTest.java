package vdt.se.demo.adapter.out.elasticsearch;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.adapter.config.AppProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ElasticsearchHttpClientTest {

    @Test
    void searchesConfiguredIndexUsingRawDsl() throws Exception {
        RestTemplate restTemplate = mock(RestTemplate.class);
        AppProperties properties = new AppProperties();
        properties.getElasticsearch().setEventsIndex("custom-events");
        when(restTemplate.postForObject(
                eq("http://localhost:9200/custom-events/_search"),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn("{\"hits\":{\"hits\":[]}}");
        ElasticsearchHttpClient client = new ElasticsearchHttpClient(
                new ObjectMapper(), restTemplate, properties, "http://localhost:9200/"
        );

        JsonNode response = client.search(new ObjectMapper().readTree("{\"query\":{\"match_all\":{}}}"));

        assertThat(response.get("hits")).isNotNull();
    }
}
