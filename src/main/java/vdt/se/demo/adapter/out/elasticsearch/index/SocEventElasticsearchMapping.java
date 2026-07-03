package vdt.se.demo.adapter.out.elasticsearch.index;

import vdt.se.demo.domain.model.SocEventSchema;

import java.util.Map;

final class SocEventElasticsearchMapping {
    private static final Map<String, String> FIELD_MAPPINGS = Map.ofEntries(
            Map.entry(SocEventSchema.TIMESTAMP, "{ \"type\": \"date\" }"),
            Map.entry(SocEventSchema.SOURCE, "{ \"type\": \"keyword\" }"),
            Map.entry(SocEventSchema.SEVERITY, "{ \"type\": \"keyword\" }"),
            Map.entry(SocEventSchema.EVENT_TYPE, "{ \"type\": \"keyword\" }"),
            Map.entry(SocEventSchema.ACTION, "{ \"type\": \"keyword\" }"),
            Map.entry(SocEventSchema.USER, "{ \"type\": \"keyword\" }"),
            Map.entry(SocEventSchema.HOST, "{ \"type\": \"keyword\" }"),
            Map.entry(SocEventSchema.IP, "{ \"type\": \"ip\", \"ignore_malformed\": true }"),
            Map.entry(SocEventSchema.GEO_LOCATION, "{ \"type\": \"keyword\" }"),
            Map.entry(SocEventSchema.USER_AGENT, "{ \"type\": \"keyword\" }"),
            Map.entry(SocEventSchema.MESSAGE, "{ \"type\": \"text\" }"),
            Map.entry(SocEventSchema.RAW, "{ \"type\": \"text\" }"),
            Map.entry(SocEventSchema.METADATA, "{ \"type\": \"flattened\" }")
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
