package vdt.se.demo.adapter.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.application.port.outboundPort.EventSpoolPort;
import vdt.se.demo.application.service.EventIngestService;

@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class AppConfig {

    @Bean
    RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    EventIngestService eventIngestService(EventSpoolPort eventSpoolPort) {
        return new EventIngestService(eventSpoolPort);
    }
}
