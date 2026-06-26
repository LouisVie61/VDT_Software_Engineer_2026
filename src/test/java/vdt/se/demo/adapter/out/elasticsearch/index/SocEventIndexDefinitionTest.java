package vdt.se.demo.adapter.out.elasticsearch.index;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class SocEventIndexDefinitionTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void mapsDemandFieldsAndMetadata() {
        JsonNode mapping = objectMapper.readTree(new SocEventIndexDefinition().json());
        JsonNode properties = mapping.get("mappings").get("properties");

        for (String field : java.util.List.of(
                "timestamp", "source", "severity", "event_type", "user", "host", "ip",
                "geo_location", "user_agent", "message", "raw")) {
            assertThat(properties.get(field)).isNotNull();
        }
        assertThat(properties.get("ip").get("type").asString()).isEqualTo("ip");
        assertThat(properties.get("ip").get("ignore_malformed").asBoolean()).isTrue();
        assertThat(properties.get("geo_location").get("type").asString()).isEqualTo("keyword");
        assertThat(properties.get("user_agent").get("type").asString()).isEqualTo("keyword");
        JsonNode metadata = properties.get("metadata");
        assertThat(metadata.get("type").asString()).isEqualTo("flattened");
    }
}
