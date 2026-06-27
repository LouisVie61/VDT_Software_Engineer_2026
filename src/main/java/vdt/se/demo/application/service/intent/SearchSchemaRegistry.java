package vdt.se.demo.application.service.intent;

import vdt.se.demo.domain.model.SocEventSchema;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class SearchSchemaRegistry {

    private static final Map<String, String> FIELD_ALIASES = Map.ofEntries(
            Map.entry("location", SocEventSchema.GEO_LOCATION),
            Map.entry("locations", SocEventSchema.GEO_LOCATION),
            Map.entry("geo location", SocEventSchema.GEO_LOCATION),
            Map.entry("geo-location", SocEventSchema.GEO_LOCATION),
            Map.entry("city", SocEventSchema.GEO_LOCATION),
            Map.entry("province", SocEventSchema.GEO_LOCATION),
            Map.entry("country", SocEventSchema.GEO_LOCATION),
            Map.entry("region", SocEventSchema.GEO_LOCATION),
            Map.entry("state", SocEventSchema.GEO_LOCATION),
            Map.entry("dia diem", SocEventSchema.GEO_LOCATION),
            Map.entry("địa điểm", SocEventSchema.GEO_LOCATION),
            Map.entry("vi tri", SocEventSchema.GEO_LOCATION),
            Map.entry("vị trí", SocEventSchema.GEO_LOCATION)
    );

    public Optional<String> canonicalField(String field) {
        String normalized = normalize(field);
        if (normalized.isBlank()) {
            return Optional.empty();
        }

        String aliased = FIELD_ALIASES.getOrDefault(normalized, normalized);

        if (SocEventSchema.FIELD_WHITELIST.contains(aliased)) {
            return Optional.of(aliased);
        }

        return Optional.empty();
    }

    public boolean isFilterable(String field) {
        return canonicalField(field)
                .map(SocEventSchema.FILTERABLE_FIELDS::contains)
                .orElse(false);
    }

    public boolean isGroupable(String field) {
        return canonicalField(field)
                .map(SocEventSchema.GROUPABLE_FIELDS::contains)
                .orElse(false);
    }

    public String requireCanonicalField(String field) {
        return canonicalField(field)
                .orElseThrow(() -> new IllegalArgumentException("Unknown field: " + field));
    }

    private String normalize(String field) {
        return field == null ? "" : field.trim().toLowerCase(Locale.ROOT);
    }
}
