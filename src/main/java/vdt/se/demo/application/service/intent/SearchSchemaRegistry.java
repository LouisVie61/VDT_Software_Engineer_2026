package vdt.se.demo.application.service.intent;

import java.util.Locale;
import java.util.Set;

public class SearchSchemaRegistry {
    private static final Set<String> FILTERABLE_FIELDS = Set.of(
            "severity", "event_type", "action", "user", "host", "ip", "source"
    );
    private static final Set<String> GROUPABLE_FIELDS = Set.of(
            "source", "severity", "event_type", "action", "user", "host", "ip"
    );

    public boolean isFilterable(String field) {
        return FILTERABLE_FIELDS.contains(normalize(field));
    }

    public boolean isGroupable(String field) {
        return GROUPABLE_FIELDS.contains(normalize(field));
    }

    private String normalize(String field) {
        return field == null ? "" : field.trim().toLowerCase(Locale.ROOT);
    }
}
