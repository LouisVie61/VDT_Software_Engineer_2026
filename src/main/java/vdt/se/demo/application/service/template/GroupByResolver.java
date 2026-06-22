package vdt.se.demo.application.service.template;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class GroupByResolver {
    public Optional<String> resolve(String query) {
        String text = normalizeAscii(query);
        if (containsAny(text, "event type", "event_type", "loai event")) {
            return Optional.of("event_type");
        }
        for (String field : List.of("ip", "user", "host", "action", "source", "severity")) {
            if (containsAny(text, field)) {
                return Optional.of(field);
            }
        }
        return Optional.empty();
    }

    private boolean containsAny(String text, String... values) {
        for (String value : values) {
            if (text.contains(value)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeAscii(String value) {
        String asciiD = (value == null ? "" : value).replace('\u0111', 'd').replace('\u0110', 'D');
        String normalized = Normalizer.normalize(asciiD, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
    }
}
