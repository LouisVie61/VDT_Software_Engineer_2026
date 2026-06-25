package vdt.se.demo.application.service.intent;

import vdt.se.demo.domain.model.SearchIntent;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class TemporalValueResolver {
    private static final DateTimeFormatter US_DATE = DateTimeFormatter.ofPattern("M/d/yyyy");
    private static final DateTimeFormatter VI_DATE = DateTimeFormatter.ofPattern("d/M/yyyy");

    public boolean apply(String text, SearchIntent intent) {
        return applyVietnameseLongDate(text, intent) || applyExactDate(text, intent) || applyYear(text, intent) || applyRelative(text, intent)
                || applyToday(text, intent);
    }

    private boolean applyVietnameseLongDate(String text, SearchIntent intent) {
        List<String> tokens = tokens(text);
        for (int i = 0; i < tokens.size(); i++) {
            int dayIndex = "ngay".equals(tokens.get(i)) ? i + 1 : i;
            if (dayIndex + 4 < tokens.size()
                    && isInteger(tokens.get(dayIndex))
                    && "thang".equals(tokens.get(dayIndex + 1))
                    && isInteger(tokens.get(dayIndex + 2))
                    && "nam".equals(tokens.get(dayIndex + 3))
                    && isYear(tokens.get(dayIndex + 4))) {
                LocalDate date = LocalDate.of(
                        Integer.parseInt(tokens.get(dayIndex + 4)),
                        Integer.parseInt(tokens.get(dayIndex + 2)),
                        Integer.parseInt(tokens.get(dayIndex))
                );
                applyDay(date, intent);
                return true;
            }
        }
        return false;
    }

    private boolean applyExactDate(String text, SearchIntent intent) {
        Optional<LocalDate> parsed = tokens(text).stream()
                .filter(this::isSlashDateWithYear)
                .findFirst()
                .flatMap(this::parseDate);
        parsed.ifPresent(date -> applyDay(date, intent));
        return parsed.isPresent();
    }

    private boolean applyYear(String text, SearchIntent intent) {
        Optional<String> year = tokens(text).stream().filter(this::isYear).findFirst();
        if (year.isEmpty()) {
            return false;
        }
        int value = Integer.parseInt(year.get());
        intent.setTimeFrom("%d-01-01T00:00:00Z".formatted(value));
        intent.setTimeTo("%d-01-01T00:00:00Z".formatted(value + 1));
        return true;
    }

    private boolean applyRelative(String text, SearchIntent intent) {
        List<String> tokens = tokens(text);
        int numberIndex = !tokens.isEmpty() && isRelativePrefix(tokens.getFirst()) ? 1 : 0;
        int unitIndex = numberIndex + 1;
        if (numberIndex < tokens.size() && isCompactDuration(tokens.get(numberIndex))) {
            int split = firstNonDigitIndex(tokens.get(numberIndex));
            return applyRelative(Integer.parseInt(tokens.get(numberIndex).substring(0, split)),
                    tokens.get(numberIndex).substring(split), intent);
        }
        if (unitIndex >= tokens.size() || !isInteger(tokens.get(numberIndex)) || !isRelativeUnit(tokens.get(unitIndex))) {
            return false;
        }
        return applyRelative(Integer.parseInt(tokens.get(numberIndex)), tokens.get(unitIndex), intent);
    }

    private boolean applyRelative(int amount, String unit, SearchIntent intent) {
        ChronoUnit chronoUnit = unit.startsWith("h") || unit.startsWith("gio") ? ChronoUnit.HOURS : ChronoUnit.DAYS;
        Instant to = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        intent.setTimeFrom(to.minus(amount, chronoUnit).toString());
        intent.setTimeTo(to.toString());
        return true;
    }

    private boolean applyToday(String text, SearchIntent intent) {
        String value = value(text);
        if (!value.equals("today") && !value.equals("hom nay") && !value.equals("ngay hom nay")
                && !value.equals("yesterday") && !value.equals("hom qua") && !value.equals("ngay hom qua")) {
            return false;
        }
        LocalDate date = value.equals("yesterday") || value.equals("hom qua") || value.equals("ngay hom qua")
                ? LocalDate.now(ZoneOffset.UTC).minusDays(1)
                : LocalDate.now(ZoneOffset.UTC);
        applyDay(date, intent);
        return true;
    }

    private void applyDay(LocalDate date, SearchIntent intent) {
        intent.setTimeFrom(date.atStartOfDay().toInstant(ZoneOffset.UTC).toString());
        intent.setTimeTo(date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).toString());
    }

    private Optional<LocalDate> parseDate(String value) {
        for (DateTimeFormatter formatter : new DateTimeFormatter[]{US_DATE, VI_DATE}) {
            try {
                return Optional.of(LocalDate.parse(value, formatter));
            } catch (DateTimeParseException ignored) {
                // Try next accepted domain date format.
            }
        }
        return Optional.empty();
    }

    private List<String> tokens(String value) {
        String normalized = value(value).replaceAll("[^a-z0-9._:/-]+", " ").trim();
        return normalized.isBlank() ? List.of() : Arrays.asList(normalized.split("\\s+"));
    }

    private boolean isSlashDateWithYear(String value) {
        String[] parts = value.split("/");
        return parts.length == 3 && Arrays.stream(parts).allMatch(this::isInteger);
    }

    private boolean isYear(String value) {
        return value.length() == 4 && isInteger(value) && (value.startsWith("19") || value.startsWith("20"));
    }

    private boolean isInteger(String value) {
        return value != null && !value.isBlank() && value.chars().allMatch(Character::isDigit);
    }

    private boolean isRelativePrefix(String value) {
        return "last".equals(value) || "past".equals(value) || "previous".equals(value) || "trong".equals(value);
    }

    private boolean isRelativeUnit(String value) {
        return switch (value) {
            case "h", "hr", "hrs", "hour", "hours", "d", "day", "days", "gio", "ngay" -> true;
            default -> false;
        };
    }

    private boolean isCompactDuration(String value) {
        int split = firstNonDigitIndex(value);
        return split > 0 && split < value.length()
                && isInteger(value.substring(0, split))
                && isRelativeUnit(value.substring(split));
    }

    private int firstNonDigitIndex(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return i;
            }
        }
        return value.length();
    }

    private String value(String value) {
        return value == null ? "" : value.toLowerCase(java.util.Locale.ROOT);
    }
}
