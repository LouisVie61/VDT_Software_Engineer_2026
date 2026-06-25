package vdt.se.demo.application.service.intent;

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

    private String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
