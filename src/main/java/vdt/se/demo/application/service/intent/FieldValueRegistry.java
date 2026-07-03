package vdt.se.demo.application.service.intent;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class FieldValueRegistry {
    private static final Map<String, Set<String>> CONTROLLED_VALUES = Map.of(
            "severity", Set.of("critical", "high", "medium", "low", "info")
    );

    public boolean isControlledField(String field) {
        return field != null && CONTROLLED_VALUES.containsKey(normalize(field));
    }

    public Optional<String> normalizeAllowedValue(String field, String value) {
        if (field == null || value == null) {
            return Optional.empty();
        }
        String normalizedField = normalize(field);
        String normalizedValue = normalize(value);
        Set<String> allowedValues = CONTROLLED_VALUES.get(normalizedField);
        if (allowedValues == null) {
            return Optional.of(value.trim());
        }
        return allowedValues.contains(normalizedValue) ? Optional.of(normalizedValue) : Optional.empty();
    }

    public Optional<String> normalizeFreeFormValue(String field, String value) {
        if (field == null || value == null || value.isBlank()) {
            return Optional.empty();
        }
        String trimmed = value.trim();
        if ("geo_location".equals(normalize(field))) {
            return normalizeGeoLocation(trimmed);
        }
        return Optional.of(trimmed);
    }

    private Optional<String> normalizeGeoLocation(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .toLowerCase(Locale.ROOT)
                .replace('\u0111', 'd')
                .replace('\u0110', 'd')
                .replaceAll("\\p{M}+", "")
                .trim();
        if (normalized.equals("vietnam") || normalized.equals("viet nam") || normalized.equals("vi?t nam")
                || normalized.equals("vi?tnam") || normalized.equals("viet-nam")) {
            return Optional.of("Vietnam");
        }
        if (value.contains("?") || value.contains("\uFFFD")) {
            return Optional.empty();
        }
        return Optional.of(value.trim());
    }

    private String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
