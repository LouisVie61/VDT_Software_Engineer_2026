package vdt.se.demo.application.service.intent;

import vdt.se.demo.domain.model.SemanticSpan;

import java.util.ArrayList;
import java.util.List;

public class PhraseSpanDetector {
    public List<SemanticSpan> detect(List<SemanticToken> tokens) {
        List<SemanticSpan> spans = new ArrayList<>();
        for (int i = 0; i < tokens.size(); i++) {
            addTopN(tokens, i, spans);
            addBucket(tokens, i, spans);
            addField(tokens, i, spans);
        }
        return spans;
    }

    private void addTopN(List<SemanticToken> tokens, int i, List<SemanticSpan> spans) {
        if (tokens.get(i).is("top") && inRange(tokens, i + 1) && tokens.get(i + 1).isInteger()) {
            spans.add(span(tokens, i, i + 1, SemanticSpan.Kind.OPERATION, "top_n"));
        }
    }

    private void addBucket(List<SemanticToken> tokens, int i, List<SemanticSpan> spans) {
        if (!tokens.get(i).is("theo") || !inRange(tokens, i + 1)) {
            return;
        }
        SemanticToken unit = tokens.get(i + 1);
        if (unit.isAny("gio", "ngay", "thang", "nam", "hour", "day", "month", "year")) {
            spans.add(span(tokens, i, i + 1, SemanticSpan.Kind.TIME_BUCKET, null));
        }
    }

    private void addField(List<SemanticToken> tokens, int i, List<SemanticSpan> spans) {
        SemanticToken token = tokens.get(i);
        if (token.isAny("source", "src", "destination", "dst") && nextIs(tokens, i, "ip")) {
            spans.add(span(tokens, i, i + 1, SemanticSpan.Kind.FIELD, "ip"));
        } else if (token.is("event") && nextIs(tokens, i, "type")) {
            spans.add(span(tokens, i, i + 1, SemanticSpan.Kind.FIELD, "event_type"));
        } else if (token.is("muc") && nextIs(tokens, i, "do")) {
            spans.add(span(tokens, i, i + 1, SemanticSpan.Kind.FIELD, "severity"));
        }
    }

    private boolean nextIs(List<SemanticToken> tokens, int i, String value) {
        return inRange(tokens, i + 1) && tokens.get(i + 1).is(value);
    }

    private boolean inRange(List<SemanticToken> tokens, int i) {
        return i >= 0 && i < tokens.size();
    }

    private SemanticSpan span(List<SemanticToken> tokens, int startIndex, int endIndex,
                              SemanticSpan.Kind kind, String canonical) {
        SemanticToken start = tokens.get(startIndex);
        SemanticToken end = tokens.get(endIndex);
        return SemanticSpan.builder()
                .kind(kind)
                .status(SemanticSpan.Status.RESOLVED)
                .text(join(tokens, startIndex, endIndex))
                .canonical(canonical)
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
