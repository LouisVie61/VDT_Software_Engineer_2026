package vdt.se.demo.adapter.out.llm;

import vdt.se.demo.domain.model.SocEventSchema;

import java.util.List;
import java.util.Map;

final class SocEventPromptSchema {
    private static final Map<String, String> DESCRIPTIONS = Map.ofEntries(
            Map.entry(SocEventSchema.TIMESTAMP, "date, range filters and date_histogram only"),
            Map.entry(SocEventSchema.SOURCE, "keyword"),
            Map.entry(SocEventSchema.SEVERITY, "keyword"),
            Map.entry(SocEventSchema.EVENT_TYPE, "keyword"),
            Map.entry(SocEventSchema.ACTION, "keyword"),
            Map.entry(SocEventSchema.USER, "keyword"),
            Map.entry(SocEventSchema.HOST, "keyword"),
            Map.entry(SocEventSchema.IP, "ip"),
            Map.entry(SocEventSchema.GEO_LOCATION, "keyword, country/location extracted from advanced_metadata.geo_location"),
            Map.entry(SocEventSchema.USER_AGENT, "keyword, browser/client extracted from advanced_metadata.user_agent"),
            Map.entry(SocEventSchema.MESSAGE, "text, free-text search only"),
            Map.entry(SocEventSchema.RAW, "not indexed, never query/filter/aggregate")
    );

    private SocEventPromptSchema() {
    }

    static String allowedFields() {
        return describedFields(List.of(
                SocEventSchema.TIMESTAMP, SocEventSchema.SOURCE, SocEventSchema.SEVERITY,
                SocEventSchema.EVENT_TYPE, SocEventSchema.ACTION, SocEventSchema.USER,
                SocEventSchema.HOST, SocEventSchema.IP, SocEventSchema.GEO_LOCATION,
                SocEventSchema.USER_AGENT, SocEventSchema.MESSAGE, SocEventSchema.RAW
        ));
    }

    static String groupableFields() {
        return String.join(", ", SocEventSchema.GROUPABLE_FIELDS);
    }

    static String filterableFieldBullets() {
        return bulletList(List.of(
                SocEventSchema.SOURCE, SocEventSchema.SEVERITY, SocEventSchema.EVENT_TYPE,
                SocEventSchema.ACTION, SocEventSchema.USER, SocEventSchema.HOST,
                SocEventSchema.IP, SocEventSchema.GEO_LOCATION, SocEventSchema.USER_AGENT
        ));
    }

    static String groupableFieldBullets() {
        return bulletList(List.of(
                SocEventSchema.SOURCE, SocEventSchema.SEVERITY, SocEventSchema.EVENT_TYPE,
                SocEventSchema.ACTION, SocEventSchema.USER, SocEventSchema.HOST,
                SocEventSchema.IP, SocEventSchema.GEO_LOCATION, SocEventSchema.USER_AGENT
        ));
    }

    static String fullTextFields() {
        return SocEventSchema.FULL_TEXT_FIELDS.toString();
    }

    private static String describedFields(List<String> fields) {
        StringBuilder builder = new StringBuilder();
        for (String field : fields) {
            builder.append("- ")
                    .append(field)
                    .append(": ")
                    .append(DESCRIPTIONS.get(field))
                    .append("\n");
        }
        return builder.toString().stripTrailing();
    }

    private static String bulletList(List<String> fields) {
        StringBuilder builder = new StringBuilder();
        for (String field : fields) {
            builder.append("- ").append(field).append("\n");
        }
        return builder.toString().stripTrailing();
    }
}
