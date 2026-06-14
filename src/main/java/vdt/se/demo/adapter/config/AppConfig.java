package vdt.se.demo.adapter.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.application.port.outboundPort.EventIndexPort;
import vdt.se.demo.application.service.EventIngestService;
import vdt.se.demo.application.service.ingest.CsvEventFileReader;
import vdt.se.demo.application.service.ingest.CsvEventParser;
import vdt.se.demo.application.service.ingest.EventFileFormatDetector;
import vdt.se.demo.application.service.ingest.JsonlEventFileReader;
import vdt.se.demo.application.service.ingest.JsonlEventParser;
import vdt.se.demo.domain.service.SocEventMapper;

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
    SocEventMapper socEventMapper() {
        return new SocEventMapper();
    }

    @Bean
    EventFileFormatDetector eventFileFormatDetector() {
        return new EventFileFormatDetector();
    }

    @Bean
    JsonlEventParser jsonlEventParser(ObjectMapper objectMapper, SocEventMapper socEventMapper) {
        return new JsonlEventParser(objectMapper, socEventMapper);
    }

    @Bean
    CsvEventParser csvEventParser(SocEventMapper socEventMapper) {
        return new CsvEventParser(socEventMapper);
    }

    @Bean
    JsonlEventFileReader jsonlEventFileReader(JsonlEventParser parser) {
        return new JsonlEventFileReader(parser);
    }

    @Bean
    CsvEventFileReader csvEventFileReader(CsvEventParser parser) {
        return new CsvEventFileReader(parser);
    }

    @Bean
    EventIngestService eventIngestService(EventFileFormatDetector formatDetector, JsonlEventFileReader jsonlReader,
                                          CsvEventFileReader csvReader, EventIndexPort eventIndexPort,
                                          AppProperties properties) {
        return new EventIngestService(
                formatDetector,
                jsonlReader,
                csvReader,
                eventIndexPort,
                properties.getIngest().getBatchSize()
        );
    }
}
