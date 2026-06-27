package vdt.se.demo.application.service.intent;

import vdt.se.demo.domain.model.SearchIntent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class SearchFilterValidator {
    private final SearchSchemaRegistry schemaRegistry;
    private final FieldValueRegistry fieldValueRegistry;

    public SearchFilterValidator() {
        this(new SearchSchemaRegistry(), new FieldValueRegistry());
    }

    public SearchFilterValidator(SearchSchemaRegistry schemaRegistry, FieldValueRegistry fieldValueRegistry) {
        this.schemaRegistry = schemaRegistry;
        this.fieldValueRegistry = fieldValueRegistry;
    }

    public void validateAndNormalize(SearchIntent intent) {
        if (intent == null || intent.getFilters() == null) {
            return;
        }

        Map<String, String> normalizedFilters = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : intent.getFilters().entrySet()) {
            String field = entry.getKey();
            String value = entry.getValue() == null ? null : entry.getValue().trim();
            Optional<String> canonicalField = schemaRegistry.canonicalField(field);

            if (value == null || value.isBlank() || canonicalField.isEmpty()
                    || !schemaRegistry.isFilterable(canonicalField.get())) {
                continue;
            }

            String normalizedField = canonicalField.get();
            if (fieldValueRegistry.isControlledField(normalizedField)) {
                fieldValueRegistry.normalizeAllowedValue(normalizedField, value)
                        .ifPresent(normalizedValue -> normalizedFilters.put(normalizedField, normalizedValue));
            } else {
                fieldValueRegistry.normalizeFreeFormValue(normalizedField, value)
                        .ifPresent(normalizedValue -> normalizedFilters.put(normalizedField, normalizedValue));
            }
        }
        intent.setFilters(normalizedFilters);
    }
}
