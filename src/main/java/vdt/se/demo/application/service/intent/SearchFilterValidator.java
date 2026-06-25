package vdt.se.demo.application.service.intent;

import vdt.se.demo.domain.model.SearchIntent;

import java.util.Iterator;
import java.util.Map;

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

        Iterator<Map.Entry<String, String>> iterator = intent.getFilters().entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            String field = entry.getKey();
            String value = entry.getValue() == null ? null : entry.getValue().trim();

            if (value == null || value.isBlank() || !schemaRegistry.isFilterable(field)) {
                iterator.remove();
                continue;
            }

            if (fieldValueRegistry.isControlledField(field)) {
                fieldValueRegistry.normalizeAllowedValue(field, value)
                        .ifPresentOrElse(entry::setValue, iterator::remove);
            } else {
                entry.setValue(value);
            }
        }
    }
}
