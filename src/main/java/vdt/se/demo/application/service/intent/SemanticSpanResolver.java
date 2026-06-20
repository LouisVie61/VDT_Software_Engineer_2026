package vdt.se.demo.application.service.intent;

import vdt.se.demo.domain.model.SemanticSpan;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class SemanticSpanResolver {
    private final SemanticAliasLexicon lexicon;
    private final SemanticTokenizer tokenizer;
    private final TemporalSpanDetector temporalSpanDetector;
    private final PhraseSpanDetector phraseSpanDetector;

    public SemanticSpanResolver() {
        this(new SemanticAliasLexicon(), new SemanticTokenizer(), new TemporalSpanDetector(), new PhraseSpanDetector());
    }

    public SemanticSpanResolver(SemanticAliasLexicon lexicon, SemanticTokenizer tokenizer,
                                TemporalSpanDetector temporalSpanDetector, PhraseSpanDetector phraseSpanDetector) {
        this.lexicon = lexicon;
        this.tokenizer = tokenizer;
        this.temporalSpanDetector = temporalSpanDetector;
        this.phraseSpanDetector = phraseSpanDetector;
    }

    public ResolvedSpans resolve(String query) {
        SemanticTokenizer.TokenizedQuery tokenized = tokenizer.tokenize(query);
        List<SemanticSpan> spans = new ArrayList<>();
        spans.addAll(temporalSpanDetector.detect(tokenized.tokens()));
        spans.addAll(phraseSpanDetector.detect(tokenized.tokens()));
        spans.addAll(lexiconSpans(tokenized.tokens()));
        return new ResolvedSpans(tokenized.normalizedQuery(), merge(spans));
    }

    private List<SemanticSpan> lexiconSpans(List<SemanticToken> tokens) {
        List<SemanticSpan> spans = new ArrayList<>();
        for (SemanticToken token : tokens) {
            Optional<SemanticAliasLexicon.ResolvedAlias> alias = lexicon.resolve(token.text());
            alias.ifPresent(resolved -> spans.add(SemanticSpan.builder()
                    .kind(kind(resolved.kind()))
                    .status(SemanticSpan.Status.RESOLVED)
                    .text(token.text())
                    .canonical(resolved.canonical())
                    .start(token.start())
                    .end(token.end())
                    .build()));
        }
        return spans;
    }

    private List<SemanticSpan> merge(List<SemanticSpan> spans) {
        if (spans.isEmpty()) {
            return List.of();
        }
        return spans.stream()
                .sorted(Comparator.comparingInt(SemanticSpan::start)
                        .thenComparing((left, right) -> Integer.compare(right.end(), left.end())))
                .distinct()
                .toList();
    }

    private SemanticSpan.Kind kind(SemanticAliasLexicon.Kind kind) {
        return switch (kind) {
            case ACTION, SEVERITY -> SemanticSpan.Kind.FILTER;
            case AUTH -> SemanticSpan.Kind.AUTH;
            case FIELD -> SemanticSpan.Kind.FIELD;
            case COMMAND -> SemanticSpan.Kind.OPERATION;
            case TARGET -> SemanticSpan.Kind.TARGET;
            case DATE -> SemanticSpan.Kind.TEMPORAL;
        };
    }

    public record ResolvedSpans(String normalizedQuery, List<SemanticSpan> spans) {
        public String residualText() {
            if (normalizedQuery == null || normalizedQuery.isBlank()) {
                return null;
            }
            StringBuilder residual = new StringBuilder(normalizedQuery);
            for (SemanticSpan span : spans) {
                for (int i = span.start(); i < span.end(); i++) {
                    residual.setCharAt(i, ' ');
                }
            }
            String value = residual.toString()
                    .replaceAll("[^a-z0-9._:/-]+", " ")
                    .trim()
                    .replaceAll("\\s+", " ");
            return value.isBlank() ? null : value;
        }
    }
}
