package vdt.se.demo.domain.service;

import org.springframework.stereotype.Service;
import vdt.se.demo.domain.valueObjects.RelativeTimeWindow;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RelativeTimeWindowParser {

    private static final Pattern RELATIVE_WINDOW_PATTERN = Pattern.compile(
            "\\b(?:last|past|previous)\\s+(\\d+)\\s*(h|hr|hrs|hour|hours|d|day|days)\\b",
            Pattern.CASE_INSENSITIVE
    );

    public Optional<RelativeTimeWindow> parse(String question) {
        Matcher matcher = RELATIVE_WINDOW_PATTERN.matcher(question == null ? "" : question);
        if (!matcher.find()) {
            return Optional.empty();
        }
        int amount = Integer.parseInt(matcher.group(1));
        String unit = matcher.group(2).toLowerCase();
        if (unit.startsWith("h")) {
            return Optional.of(RelativeTimeWindow.ofHours(amount));
        }
        return Optional.of(RelativeTimeWindow.ofDays(amount));
    }
}
