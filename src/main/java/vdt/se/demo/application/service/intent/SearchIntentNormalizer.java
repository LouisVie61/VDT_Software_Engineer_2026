package vdt.se.demo.application.service.intent;

import vdt.se.demo.application.dto.SearchRequest;
import vdt.se.demo.domain.model.SearchIntent;
import vdt.se.demo.domain.valueObjects.TemplateType;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchIntentNormalizer {
    private static final Set<String> ALLOWED_FILTERS = Set.of(
            "severity", "event_type", "action", "user", "host", "ip", "source"
    );
    private static final Set<String> SEVERITIES = Set.of("critical", "high", "medium", "low", "info");
    private static final Set<String> AGGREGATION_NOISE_TOKENS = Set.of(
            "cac", "cua", "va", "la", "nhung", "mot", "nhieu", "loi", "error", "errors",
            "event", "events", "alert", "alerts", "log", "logs", "record", "records",
            "statistic", "statistics", "stats", "thong", "ke", "dem", "count", "top",
            "trong", "nam", "thang", "ngay", "gio", "qua"
    );

    private final SemanticResidualTextResolver residualTextResolver;
    private final SemanticSpanResolver spanResolver;
    private final SearchTimeExpressionResolver timeExpressionResolver;

    public SearchIntentNormalizer() {
        this(new SemanticResidualTextResolver(), new SemanticSpanResolver(), new SearchTimeExpressionResolver());
    }

    public SearchIntentNormalizer(SemanticResidualTextResolver residualTextResolver) {
        this(residualTextResolver, new SemanticSpanResolver(), new SearchTimeExpressionResolver());
    }

    public SearchIntentNormalizer(SemanticResidualTextResolver residualTextResolver,
                                  SemanticSpanResolver spanResolver,
                                  SearchTimeExpressionResolver timeExpressionResolver) {
        this.residualTextResolver = residualTextResolver;
        this.spanResolver = spanResolver;
        this.timeExpressionResolver = timeExpressionResolver;
    }

    public SearchIntent normalize(SearchRequest request, SearchIntent input) {
        SearchIntent intent = input == null ? new SearchIntent() : input.copy();
        intent.setSemanticSpans(spanResolver.resolve(request.getQuestion()).spans());
        intent.setFilters(normalizedFilters(intent.getFilters()));
        removeFiltersOverriddenByRequest(request, intent);

        inferFilters(request, intent);
        timeExpressionResolver.apply(request, intent);
        intent.setTextQuery(normalizedTextQuery(request, intent));

        if (intent.getIntent() == null) {
            intent.setIntent(TemplateType.SIMPLE_SEARCH);
        }
        if (intent.getIntent() == TemplateType.TIME_AGGREGATION && !hasText(intent.getTimeBucket())) {
            boolean broadRange = hasText(intent.getTimeFrom());
            intent.setTimeBucket(timeExpressionResolver.inferBucket(intent.getSemanticSpans(), broadRange));
        }
        if (intent.getIntent() == TemplateType.TERMS_AGGREGATION && intent.getTopN() == null) {
            intent.setTopN(inferTopN(request.getQuestion()));
        }
        return intent;
    }

    private Map<String, String> normalizedFilters(Map<String, String> rawFilters) {
        Map<String, String> filters = new LinkedHashMap<>();
        if (rawFilters == null) {
            return filters;
        }
        rawFilters.forEach((field, value) -> {
            String normalizedField = canonicalize(field).replace(' ', '_');
            if (ALLOWED_FILTERS.contains(normalizedField) && hasText(value)) {
                filters.put(normalizedField, value.trim());
            }
        });
        return filters;
    }

    private void removeFiltersOverriddenByRequest(SearchRequest request, SearchIntent intent) {
        removeIfRequestHasValue(intent, "severity", request.getSeverity());
        removeIfRequestHasValue(intent, "event_type", request.getEventType());
        removeIfRequestHasValue(intent, "user", request.getUser());
        removeIfRequestHasValue(intent, "host", request.getHost());
        removeIfRequestHasValue(intent, "ip", request.getIp());
    }

    private void removeIfRequestHasValue(SearchIntent intent, String field, String value) {
        if (hasText(value)) {
            intent.getFilters().remove(field);
        }
    }

    private void inferFilters(SearchRequest request, SearchIntent intent) {
        String text = canonicalize(value(request.getQuestion()) + " " + value(intent.getTextQuery()));
        if (!hasText(request.getSeverity()) && !intent.getFilters().containsKey("severity")) {
            inferSeverity(text, intent);
        }
        if (!hasText(request.getEventType()) && !intent.getFilters().containsKey("event_type")
                && residualTextResolver.looksLikeAuthSearch(text)) {
            intent.getFilters().put("event_type", "auth");
        }
    }

    private void inferSeverity(String text, SearchIntent intent) {
        for (String token : text.split("\\s+")) {
            if (SEVERITIES.contains(token)) {
                intent.getFilters().put("severity", token);
                return;
            }
        }
    }

    private String normalizedTextQuery(SearchRequest request, SearchIntent intent) {
        SemanticResidualTextResolver.Resolution resolution = residualTextResolver.resolve(
                request.getQuestion(), intent.getTextQuery());
        resolution.inferredFilters().forEach((field, value) -> {
            if (!intent.getFilters().containsKey(field)) {
                intent.getFilters().put(field, value);
            }
        });
        return residualTextForIntent(intent, resolution.textQuery());
    }

    private String residualTextForIntent(SearchIntent intent, String textQuery) {
        if (!hasText(textQuery) || intent == null || intent.getIntent() == TemplateType.SIMPLE_SEARCH) {
            return textQuery;
        }

        StringBuilder kept = new StringBuilder();
        for (String token : canonicalize(textQuery).split("\\s+")) {
            if (!hasText(token) || AGGREGATION_NOISE_TOKENS.contains(token) || !isInformativeAggregationToken(token)) {
                continue;
            }
            if (!kept.isEmpty()) {
                kept.append(' ');
            }
            kept.append(token);
        }
        return kept.isEmpty() ? null : kept.toString();
    }

    private boolean isInformativeAggregationToken(String token) {
        if (token.matches("\\d{1,3}(?:\\.\\d{1,3}){3}")) {
            return true;
        }
        if (token.matches(".*[._:/-].*") || token.matches(".*\\d.*")) {
            return true;
        }
        return token.length() >= 4;
    }

    private Integer inferTopN(String question) {
        Matcher matcher = Pattern.compile("\\btop\\s+(\\d+)\\b").matcher(canonicalize(question));
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 10;
    }

    private String canonicalize(String value) {
        String asciiD = value(value).replace('\u0111', 'd').replace('\u0110', 'D');
        String normalized = Normalizer.normalize(asciiD, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}+", "").toLowerCase(java.util.Locale.ROOT);
    }

    private String value(String value) {
        return value == null ? "" : value;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
