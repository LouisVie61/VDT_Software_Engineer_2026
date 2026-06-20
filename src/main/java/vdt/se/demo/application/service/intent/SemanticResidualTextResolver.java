package vdt.se.demo.application.service.intent;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import vdt.se.demo.domain.model.SemanticSpan;

public class SemanticResidualTextResolver {
    private static final Set<String> ACTION_TEXT = Set.of("success", "failed", "locked", "bypass");
    private static final Set<String> STOPWORDS = Set.of(
            "show", "me", "please", "list", "find", "get", "search", "in", "on", "at", "for", "of", "the", "a", "an",
            "with", "by", "cac", "cua", "va", "la", "nhung", "mot", "nhieu"
    );

    private final SemanticAliasLexicon lexicon;
    private final SemanticSpanResolver spanResolver;

    public SemanticResidualTextResolver() {
        this(new SemanticAliasLexicon(), new SemanticSpanResolver());
    }

    public SemanticResidualTextResolver(SemanticAliasLexicon lexicon, SemanticSpanResolver spanResolver) {
        this.lexicon = lexicon;
        this.spanResolver = spanResolver;
    }

    public Resolution resolve(String question, String extractedTextQuery) {
        String normalizedQuestion = canonicalize(question);
        if (normalizedQuestion.isBlank()) {
            return Resolution.empty();
        }

        SemanticSpanResolver.ResolvedSpans resolvedSpans = spanResolver.resolve(question);
        LinkedHashMap<String, String> inferredFilters = inferredFilters(resolvedSpans);
        String residualText = cleanedResidualText(resolvedSpans.residualText());
        if (residualText == null) {
            residualText = primaryActionText(resolvedSpans);
        }

        return new Resolution(residualText, inferredFilters);
    }

    public boolean looksLikeAuthSearch(String text) {
        String normalized = canonicalize(text);
        return lexicon.containsKind(normalized, SemanticAliasLexicon.Kind.AUTH);
    }

    private LinkedHashMap<String, String> inferredFilters(SemanticSpanResolver.ResolvedSpans resolvedSpans) {
        LinkedHashMap<String, String> filters = new LinkedHashMap<>();
        for (SemanticSpan span : resolvedSpans.spans()) {
            if (span.kind() != SemanticSpan.Kind.FILTER || span.status() != SemanticSpan.Status.RESOLVED) {
                continue;
            }
            if ("critical".equals(span.canonical()) || "high".equals(span.canonical())
                    || "medium".equals(span.canonical()) || "low".equals(span.canonical())
                    || "info".equals(span.canonical())) {
                filters.put("severity", span.canonical());
            }
        }
        return filters;
    }

    private String primaryActionText(SemanticSpanResolver.ResolvedSpans resolvedSpans) {
        for (SemanticSpan span : resolvedSpans.spans()) {
            if (span.status() == SemanticSpan.Status.RESOLVED && span.canonical() != null
                    && ACTION_TEXT.contains(span.canonical())) {
                return span.canonical();
            }
        }
        return null;
    }

    private String cleanedResidualText(String residualText) {
        if (residualText == null || residualText.isBlank()) {
            return null;
        }
        String cleaned = residualText.strip().replaceAll("\\s+", " ");
        StringBuilder kept = new StringBuilder();
        for (String token : cleaned.split("\\s+")) {
            if (STOPWORDS.contains(token)) {
                continue;
            }
            if (!kept.isEmpty()) {
                kept.append(' ');
            }
            kept.append(token);
        }
        return kept.isEmpty() ? null : kept.toString();
    }

    private String canonicalize(String value) {
        String asciiD = value(value).replace('\u0111', 'd').replace('\u0110', 'D');
        String normalized = Normalizer.normalize(asciiD, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
    }

    private String value(String value) {
        return value == null ? "" : value;
    }

    public record Resolution(String textQuery, Map<String, String> inferredFilters) {
        static Resolution empty() {
            return new Resolution(null, Map.of());
        }
    }
}
