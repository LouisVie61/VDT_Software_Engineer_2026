package vdt.se.demo.application.service.template;

import vdt.se.demo.domain.model.SocEventSchema;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class GroupByResolver {
    public Optional<String> resolve(String query) {
        String text = normalizeAscii(query);
        if (containsAny(text, "event type", "event_type", "loai event")) {
            return Optional.of(SocEventSchema.EVENT_TYPE);
        }
        if (containsAny(query, "location", "locations", "city", "country", "province", "region", "địa điểm", "vị trí")) {
            return Optional.of(SocEventSchema.GEO_LOCATION);
        }
        if (containsAny(text, "user agent", "user_agent", "browser", "trinh duyet")) {
            return Optional.of(SocEventSchema.USER_AGENT);
        }
        for (String field : List.of(
                SocEventSchema.IP, SocEventSchema.USER, SocEventSchema.HOST,
                SocEventSchema.ACTION, SocEventSchema.SOURCE, SocEventSchema.SEVERITY)) {
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
