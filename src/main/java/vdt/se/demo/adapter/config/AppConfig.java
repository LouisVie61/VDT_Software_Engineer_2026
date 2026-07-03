package vdt.se.demo.adapter.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.application.port.outboundPort.ingest.EventSpoolPort;
import vdt.se.demo.application.service.ingest.EventIngestService;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class AppConfig {
    private final AppProperties properties;

    public AppConfig(AppProperties properties) {
        this.properties = properties;
    }

    @Bean
    RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory requestFactory = requestFactory();
        return new RestTemplate(requestFactory);
    }

    @Bean
    RestClient restClient() {
        return RestClient.builder().requestFactory(requestFactory()).build();
    }

    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    EventIngestService eventIngestService(EventSpoolPort eventSpoolPort) {
        return new EventIngestService(eventSpoolPort);
    }

    private SimpleClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(Math.max(1, properties.getLlm().getConnectTimeoutSeconds())));
        requestFactory.setReadTimeout(Duration.ofSeconds(Math.max(1, properties.getLlm().getReadTimeoutSeconds())));
        return requestFactory;
    }
}
