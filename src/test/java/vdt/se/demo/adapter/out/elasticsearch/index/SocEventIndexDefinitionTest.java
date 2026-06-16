package vdt.se.demo.adapter.out.elasticsearch.index;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class SocEventIndexDefinitionTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void mapsMetadataAsFlattenedToAvoidDynamicObjectConflicts() {
        JsonNode mapping = objectMapper.readTree(new SocEventIndexDefinition().json());

        JsonNode metadata = mapping.get("mappings").get("properties").get("metadata");

        assertThat(metadata.get("type").asString()).isEqualTo("flattened");
    }
}
