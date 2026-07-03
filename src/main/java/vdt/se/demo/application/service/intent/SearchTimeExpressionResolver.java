package vdt.se.demo.application.service.intent;

import vdt.se.demo.application.dto.SearchRequest;
import vdt.se.demo.domain.model.SearchIntent;
import vdt.se.demo.domain.model.SemanticSpan;

import java.util.List;

public class SearchTimeExpressionResolver {
    private final TemporalValueResolver temporalValueResolver;
    private final SemanticSpanResolver semanticSpanResolver;

    public SearchTimeExpressionResolver() {
        this(new TemporalValueResolver(), new SemanticSpanResolver());
    }

    public SearchTimeExpressionResolver(TemporalValueResolver temporalValueResolver) {
        this(temporalValueResolver, new SemanticSpanResolver());
    }

    public SearchTimeExpressionResolver(TemporalValueResolver temporalValueResolver,
                                        SemanticSpanResolver semanticSpanResolver) {
        this.temporalValueResolver = temporalValueResolver;
        this.semanticSpanResolver = semanticSpanResolver;
    }

    public void apply(SearchRequest request, SearchIntent intent) {
        intent.setSemanticSpans(resolveTemporalStatuses(temporalSpans(request, intent)));
        if (hasText(request.getFrom()) || hasText(request.getTo())
                || hasText(intent.getTimeFrom()) || hasText(intent.getTimeTo())) {
            return;
        }
        for (SemanticSpan span : safe(intent.getSemanticSpans())) {
            if (span.kind() == SemanticSpan.Kind.TEMPORAL
                    && span.status() != SemanticSpan.Status.UNSUPPORTED
                    && temporalValueResolver.apply(span.text(), intent)) {
                return;
            }
        }
    }

    public String inferBucket(List<SemanticSpan> spans, boolean broadRange) {
        for (SemanticSpan span : safe(spans)) {
            if (span.kind() == SemanticSpan.Kind.TIME_BUCKET) {
                String bucket = bucket(span.text());
                if (hasText(bucket)) {
                    return bucket;
                }
            }
        }
        return broadRange ? "30d" : "1h";
    }

    private List<SemanticSpan> resolveTemporalStatuses(List<SemanticSpan> spans) {
        return safe(spans).stream()
                .map(span -> span.kind() == SemanticSpan.Kind.TEMPORAL && temporalValueResolver.apply(span.text(), SearchIntent.builder().build())
                        ? withStatus(span, SemanticSpan.Status.RESOLVED)
                        : span)
                .toList();
    }

    private List<SemanticSpan> temporalSpans(SearchRequest request, SearchIntent intent) {
        List<SemanticSpan> spans = new java.util.ArrayList<>(safe(intent.getSemanticSpans()));
        if (request != null && hasText(request.getQuestion())) {
            semanticSpanResolver.resolve(request.getQuestion()).spans().stream()
                    .filter(span -> span.kind() == SemanticSpan.Kind.TEMPORAL)
                    .forEach(spans::add);
        }
        return spans.stream()
                .distinct()
                .toList();
    }

    private String bucket(String text) {
        String value = text == null ? "" : text.toLowerCase(java.util.Locale.ROOT);
        if (value.contains("ngay") || value.contains("day")) {
            return "1d";
        }
        if (value.contains("gio") || value.contains("hour")) {
            return "1h";
        }
        return null;
    }

    private SemanticSpan withStatus(SemanticSpan span, SemanticSpan.Status status) {
        return SemanticSpan.builder()
                .kind(span.kind())
                .status(status)
                .text(span.text())
                .canonical(span.canonical())
                .start(span.start())
                .end(span.end())
                .build();
    }

    private List<SemanticSpan> safe(List<SemanticSpan> spans) {
        return spans == null ? List.of() : spans;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
