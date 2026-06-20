package vdt.se.demo.application.service.intent;

public record SemanticToken(String text, int start, int end) {
    boolean is(String value) {
        return text.equals(value);
    }

    boolean isAny(String... values) {
        for (String value : values) {
            if (text.equals(value)) {
                return true;
            }
        }
        return false;
    }

    boolean isInteger() {
        return text.matches("\\d+");
    }

    boolean isCompactDuration() {
        int split = firstNonDigitIndex();
        return split > 0 && split < text.length()
                && text.substring(0, split).matches("\\d+")
                && isDurationUnit(text.substring(split));
    }

    int compactDurationAmount() {
        return Integer.parseInt(text.substring(0, firstNonDigitIndex()));
    }

    String compactDurationUnit() {
        return text.substring(firstNonDigitIndex());
    }

    boolean isYear() {
        return text.matches("(?:19|20)\\d{2}");
    }

    boolean isSlashDate() {
        String[] parts = text.split("/");
        return parts.length >= 2 && parts.length <= 3
                && java.util.Arrays.stream(parts).allMatch(part -> part.matches("\\d{1,4}"));
    }

    private int firstNonDigitIndex() {
        for (int i = 0; i < text.length(); i++) {
            if (!Character.isDigit(text.charAt(i))) {
                return i;
            }
        }
        return text.length();
    }

    private boolean isDurationUnit(String value) {
        return switch (value) {
            case "h", "hr", "hrs", "hour", "hours", "d", "day", "days", "gio", "ngay" -> true;
            default -> false;
        };
    }
}
