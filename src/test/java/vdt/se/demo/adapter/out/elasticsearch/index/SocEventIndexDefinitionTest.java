package vdt.se.demo.adapter.out.elasticsearch.index;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.domain.model.SocEventSchema;

import static org.assertj.core.api.Assertions.assertThat;

class SocEventIndexDefinitionTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void mapsDemandFieldsAndMetadata() {
        JsonNode mapping = objectMapper.readTree(new SocEventIndexDefinition().json());
        JsonNode properties = mapping.get("mappings").get("properties");

        for (String field : java.util.List.of(
                "timestamp", "timestamp_year", "timestamp_month", "timestamp_day", "timestamp_hour",
                "timestamp_minute", "timestamp_second", "source", "severity", "event_type", "action", "user", "host", "ip",
                "geo_location", "user_agent", "message", "raw", "metadata", "advanced_metadata")) {
            assertThat(properties.get(field)).isNotNull();
        }
        assertThat(properties.get("ip").get("type").asString()).isEqualTo("ip");
        assertThat(properties.get("ip").get("ignore_malformed").asBoolean()).isTrue();
        assertThat(properties.get("geo_location").get("type").asString()).isEqualTo("keyword");
        assertThat(properties.get("user_agent").get("type").asString()).isEqualTo("keyword");
        assertThat(properties.get("action").get("type").asString()).isEqualTo("keyword");
        assertThat(properties.get("timestamp_year").get("type").asString()).isEqualTo("integer");
        assertThat(properties.get("src_ip").get("type").asString()).isEqualTo("ip");
        assertThat(properties.get("risk_score").get("type").asString()).isEqualTo("double");
        assertThat(properties.get("frequency_anomaly").get("type").asString()).isEqualTo("boolean");
        assertThat(properties.get("source_product").get("type").asString()).isEqualTo("keyword");
        JsonNode metadata = properties.get("metadata");
        assertThat(metadata.get("type").asString()).isEqualTo("flattened");
        assertThat(properties.get("advanced_metadata").get("type").asString()).isEqualTo("flattened");
        assertThat(properties.get("raw").get("enabled").asBoolean()).isFalse();
    }

    @Test
    void mapsDerivedCapabilityFieldsAsExactOrNumericTypes() {
        JsonNode properties = objectMapper.readTree(new SocEventIndexDefinition().json()).get("mappings").get("properties");

        assertThat(properties.get("timestamp_date").get("type").asString()).isEqualTo("keyword");
        assertThat(properties.get("severity_rank").get("type").asString()).isEqualTo("integer");
        assertThat(properties.get("src_ip_prefix24").get("type").asString()).isEqualTo("keyword");
        assertThat(properties.get("network_pair").get("type").asString()).isEqualTo("keyword");
        assertThat(properties.get("user_agent_family").get("type").asString()).isEqualTo("keyword");
    }

    @Test
    void everyQueryableFieldHasAnIndexMapping() {
        assertThat(SocEventSchema.FILTERABLE_FIELDS).allMatch(SocEventSchema.INDEX_FIELDS::contains);
        assertThat(SocEventSchema.GROUPABLE_FIELDS).allMatch(SocEventSchema.INDEX_FIELDS::contains);
        assertThat(SocEventSchema.FULL_TEXT_FIELDS).allMatch(SocEventSchema.INDEX_FIELDS::contains);
        assertThat(SocEventSchema.NUMERIC_METRIC_FIELDS).allMatch(SocEventSchema.INDEX_FIELDS::contains);
        assertThat(SocEventSchema.FIELD_WHITELIST).contains(
                "src_ip", "dst_ip", "device_type", "process_id", "risk_score", "baseline_deviation",
                "source_product", "risk_level", "event_type_action");
    }
}
