package vdt.se.demo.application.service.intent;

import vdt.se.demo.domain.model.SemanticSpan;

import java.util.ArrayList;
import java.util.List;

public class TemporalSpanDetector {
    public List<SemanticSpan> detect(List<SemanticToken> tokens) {
        List<SemanticSpan> spans = new ArrayList<>();
        for (int i = 0; i < tokens.size(); i++) {
            SemanticSpan span = detectAt(tokens, i);
            if (span != null) {
                spans.add(span);
            }
        }
        return spans;
    }

    private SemanticSpan detectAt(List<SemanticToken> tokens, int i) {
        SemanticToken token = tokens.get(i);
        if (token.isSlashDate()) {
            return span(tokens, i, i, token.text().split("/").length == 3
                    ? SemanticSpan.Status.RESOLVED
                    : SemanticSpan.Status.AMBIGUOUS);
        }
        if (token.isYear()) {
            int start = previousIs(tokens, i, "nam") ? i - 1 : i;
            start = previousIs(tokens, start, "trong") ? start - 1 : start;
            return span(tokens, start, i, SemanticSpan.Status.RESOLVED);
        }
        if (isToday(tokens, i)) {
            int end = tokens.get(i).isAny("today", "yesterday") ? i : i + (tokens.get(i).is("ngay") ? 2 : 1);
            return span(tokens, i, end, SemanticSpan.Status.RESOLVED);
        }
        if (token.isCompactDuration()) {
            int end = nextIs(tokens, i, "qua") ? i + 1 : i;
            return span(tokens, i, end, SemanticSpan.Status.RESOLVED);
        }
        if (isRelativeWindow(tokens, i)) {
            int end = i + (tokens.get(i).isAny("last", "past", "previous") ? 2 : 1);
            if (nextIs(tokens, end, "qua")) {
                end++;
            }
            return span(tokens, i, end, SemanticSpan.Status.RESOLVED);
        }
        if (isDayMonth(tokens, i)) {
            int end = i + (tokens.get(i).is("ngay") ? 3 : 2);
            SemanticSpan.Status status = SemanticSpan.Status.AMBIGUOUS;
            if (nextIs(tokens, end, "hang") && nextIs(tokens, end + 1, "nam")) {
                end += 2;
                status = SemanticSpan.Status.UNSUPPORTED;
            }
            return span(tokens, i, end, status);
        }
        return null;
    }

    private boolean isToday(List<SemanticToken> tokens, int i) {
        return tokens.get(i).isAny("today", "yesterday")
                || tokens.get(i).is("hom") && nextIs(tokens, i, "nay")
                || tokens.get(i).is("ngay") && nextIs(tokens, i, "hom") && nextIs(tokens, i + 1, "nay");
    }

    private boolean isRelativeWindow(List<SemanticToken> tokens, int i) {
        int numberIndex = tokens.get(i).isAny("last", "past", "previous") ? i + 1 : i;
        int unitIndex = numberIndex + 1;
        return inRange(tokens, unitIndex)
                && tokens.get(numberIndex).isInteger()
                && tokens.get(unitIndex).isAny("h", "hr", "hrs", "hour", "hours", "d", "day", "days", "gio", "ngay");
    }

    private boolean isDayMonth(List<SemanticToken> tokens, int i) {
        int dayIndex = tokens.get(i).is("ngay") ? i + 1 : i;
        return inRange(tokens, dayIndex + 2)
                && tokens.get(dayIndex).isInteger()
                && tokens.get(dayIndex + 1).is("thang")
                && tokens.get(dayIndex + 2).isInteger();
    }

    private boolean previousIs(List<SemanticToken> tokens, int i, String value) {
        return i > 0 && tokens.get(i - 1).is(value);
    }

    private boolean nextIs(List<SemanticToken> tokens, int i, String value) {
        return inRange(tokens, i + 1) && tokens.get(i + 1).is(value);
    }

    private boolean inRange(List<SemanticToken> tokens, int i) {
        return i >= 0 && i < tokens.size();
    }

    private SemanticSpan span(List<SemanticToken> tokens, int startIndex, int endIndex, SemanticSpan.Status status) {
        SemanticToken start = tokens.get(startIndex);
        SemanticToken end = tokens.get(endIndex);
        return SemanticSpan.builder()
                .kind(SemanticSpan.Kind.TEMPORAL)
                .status(status)
                .text(join(tokens, startIndex, endIndex))
                .start(start.start())
                .end(end.end())
                .build();
    }

    private String join(List<SemanticToken> tokens, int startIndex, int endIndex) {
        StringBuilder value = new StringBuilder();
        for (int i = startIndex; i <= endIndex; i++) {
            if (!value.isEmpty()) {
                value.append(' ');
            }
            value.append(tokens.get(i).text());
        }
        return value.toString();
    }
}
