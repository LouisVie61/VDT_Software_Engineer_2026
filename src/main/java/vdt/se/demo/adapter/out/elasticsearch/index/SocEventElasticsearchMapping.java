package vdt.se.demo.adapter.out.elasticsearch.index;

import vdt.se.demo.domain.model.SocEventSchema;

import java.util.Map;

final class SocEventElasticsearchMapping {
    private static final Map<String, String> FIELD_MAPPINGS = Map.ofEntries(
            Map.entry(SocEventSchema.EVENT_ID, "{ \"type\": \"keyword\" }"),
            Map.entry(SocEventSchema.TIMESTAMP, "{ \"type\": \"date\" }"),
            Map.entry(SocEventSchema.TIMESTAMP_YEAR, "{ \"type\": \"integer\" }"),
            Map.entry(SocEventSchema.TIMESTAMP_MONTH, "{ \"type\": \"integer\" }"),
            Map.entry(SocEventSchema.TIMESTAMP_DAY, "{ \"type\": \"integer\" }"),
            Map.entry(SocEventSchema.TIMESTAMP_QUARTER, "{ \"type\": \"integer\" }"),
            Map.entry(SocEventSchema.TIMESTAMP_HOUR, "{ \"type\": \"integer\" }"),
            Map.entry(SocEventSchema.TIMESTAMP_MINUTE, "{ \"type\": \"integer\" }"),
            Map.entry(SocEventSchema.TIMESTAMP_SECOND, "{ \"type\": \"integer\" }"),
            Map.entry(SocEventSchema.TIMESTAMP_DATE, "{ \"type\": \"keyword\" }"),
            Map.entry(SocEventSchema.TIMESTAMP_DAY_OF_WEEK, "{ \"type\": \"keyword\" }"),
            Map.entry(SocEventSchema.TIMESTAMP_IS_WEEKEND, "{ \"type\": \"boolean\" }"),
            Map.entry(SocEventSchema.SOURCE, "{ \"type\": \"keyword\" }"),
            Map.entry(SocEventSchema.SOURCE_PRODUCT, "{ \"type\": \"keyword\" }"),
            Map.entry(SocEventSchema.SOURCE_VERSION, "{ \"type\": \"keyword\" }"),
            Map.entry(SocEventSchema.SEVERITY, "{ \"type\": \"keyword\" }"),
            Map.entry(SocEventSchema.SEVERITY_RANK, "{ \"type\": \"integer\" }"),
            Map.entry(SocEventSchema.EVENT_TYPE, "{ \"type\": \"keyword\" }"),
            Map.entry(SocEventSchema.ACTION, "{ \"type\": \"keyword\" }"),
            Map.entry(SocEventSchema.USER, "{ \"type\": \"keyword\" }"),
            Map.entry(SocEventSchema.HOST, "{ \"type\": \"keyword\" }"),
            Map.entry(SocEventSchema.IP, "{ \"type\": \"ip\", \"ignore_malformed\": true }"),
            Map.entry(SocEventSchema.GEO_LOCATION, "{ \"type\": \"keyword\" }"),
            Map.entry(SocEventSchema.USER_AGENT, "{ \"type\": \"keyword\" }"),
            Map.entry(SocEventSchema.USER_AGENT_FAMILY, "{ \"type\": \"keyword\" }"),
            Map.entry(SocEventSchema.USER_AGENT_OS, "{ \"type\": \"keyword\" }"),
            Map.entry(SocEventSchema.SRC_IP, "{ \"type\": \"ip\", \"ignore_malformed\": true }"),
            Map.entry(SocEventSchema.DST_IP, "{ \"type\": \"ip\", \"ignore_malformed\": true }"),
            Map.entry(SocEventSchema.SRC_IP_PREFIX24, "{ \"type\": \"keyword\" }"),
            Map.entry(SocEventSchema.DST_IP_PREFIX24, "{ \"type\": \"keyword\" }"),
            Map.entry(SocEventSchema.NETWORK_PAIR, "{ \"type\": \"keyword\" }"),
            Map.entry(SocEventSchema.ALERT_TYPE, "{ \"type\": \"keyword\" }"),
            Map.entry(SocEventSchema.SIGNATURE_ID, "{ \"type\": \"keyword\" }"),
            Map.entry(SocEventSchema.CATEGORY, "{ \"type\": \"keyword\" }"),
            Map.entry(SocEventSchema.DEVICE_TYPE, "{ \"type\": \"keyword\" }"),
            Map.entry(SocEventSchema.DEVICE_ID, "{ \"type\": \"keyword\" }"),
            Map.entry(SocEventSchema.FIRMWARE_VERSION, "{ \"type\": \"keyword\" }"),
            Map.entry(SocEventSchema.OBJECT, "{ \"type\": \"keyword\", \"ignore_above\": 2048 }"),
            Map.entry(SocEventSchema.PROCESS_ID, "{ \"type\": \"integer\" }"),
            Map.entry(SocEventSchema.PARENT_PROCESS, "{ \"type\": \"keyword\" }"),
            Map.entry(SocEventSchema.ADDITIONAL_INFO, "{ \"type\": \"text\" }"),
            Map.entry(SocEventSchema.DESCRIPTION, "{ \"type\": \"text\" }"),
            Map.entry(SocEventSchema.RAW_LOG, "{ \"type\": \"text\", \"index\": false }"),
            Map.entry(SocEventSchema.DEVICE_HASH, "{ \"type\": \"keyword\" }"),
            Map.entry(SocEventSchema.SESSION_ID, "{ \"type\": \"keyword\" }"),
            Map.entry(SocEventSchema.RISK_SCORE, "{ \"type\": \"double\" }"),
            Map.entry(SocEventSchema.RISK_LEVEL, "{ \"type\": \"keyword\" }"),
            Map.entry(SocEventSchema.CONFIDENCE, "{ \"type\": \"double\" }"),
            Map.entry(SocEventSchema.CONFIDENCE_LEVEL, "{ \"type\": \"keyword\" }"),
            Map.entry(SocEventSchema.BASELINE_DEVIATION, "{ \"type\": \"double\" }"),
            Map.entry(SocEventSchema.ENTROPY, "{ \"type\": \"double\" }"),
            Map.entry(SocEventSchema.FREQUENCY_ANOMALY, "{ \"type\": \"boolean\" }"),
            Map.entry(SocEventSchema.SEQUENCE_ANOMALY, "{ \"type\": \"boolean\" }"),
            Map.entry(SocEventSchema.HAS_BEHAVIORAL_ANOMALY, "{ \"type\": \"boolean\" }"),
            Map.entry(SocEventSchema.EVENT_TYPE_ACTION, "{ \"type\": \"keyword\" }"),
            Map.entry(SocEventSchema.SRC_PORT, "{ \"type\": \"integer\" }"),
            Map.entry(SocEventSchema.DST_PORT, "{ \"type\": \"integer\" }"),
            Map.entry(SocEventSchema.PROTOCOL, "{ \"type\": \"keyword\" }"),
            Map.entry(SocEventSchema.BYTES, "{ \"type\": \"long\" }"),
            Map.entry(SocEventSchema.DURATION, "{ \"type\": \"double\" }"),
            Map.entry(SocEventSchema.CLOUD_SERVICE, "{ \"type\": \"keyword\" }"),
            Map.entry(SocEventSchema.RESOURCE_ID, "{ \"type\": \"keyword\" }"),
            Map.entry(SocEventSchema.METHOD, "{ \"type\": \"keyword\" }"),
            Map.entry(SocEventSchema.MODEL_ID, "{ \"type\": \"keyword\" }"),
            Map.entry(SocEventSchema.INPUT_HASH, "{ \"type\": \"keyword\" }"),
            Map.entry(SocEventSchema.OUTPUT_HASH, "{ \"type\": \"keyword\" }"),
            Map.entry(SocEventSchema.MAC_ADDRESS, "{ \"type\": \"keyword\" }"),
            Map.entry(SocEventSchema.MESSAGE, "{ \"type\": \"text\" }"),
            Map.entry(SocEventSchema.RAW, "{ \"type\": \"object\", \"enabled\": false }"),
            Map.entry(SocEventSchema.METADATA, "{ \"type\": \"flattened\" }"),
            Map.entry(SocEventSchema.ADVANCED_METADATA, "{ \"type\": \"flattened\" }")
    );

    private SocEventElasticsearchMapping() {
    }

    static String propertiesJson() {
        StringBuilder properties = new StringBuilder();
        for (String field : SocEventSchema.INDEX_FIELDS) {
            if (!properties.isEmpty()) {
                properties.append(",\n");
            }
            properties.append("                      \"")
                    .append(field)
                    .append("\": ")
                    .append(FIELD_MAPPINGS.get(field));
        }
        return properties.toString();
    }
}
