package vdt.se.demo.application.service.intent;

import vdt.se.demo.application.dto.SearchRequest;
import vdt.se.demo.domain.model.SearchIntent;
import vdt.se.demo.domain.valueObjects.TemplateType;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.Map;

public class SearchIntentNormalizer {
    private final SearchTimeExpressionResolver timeExpressionResolver;
    private final SearchFilterValidator filterValidator;

    public SearchIntentNormalizer() {
        this(new SearchTimeExpressionResolver(), new SearchFilterValidator());
    }

    public SearchIntentNormalizer(SearchTimeExpressionResolver timeExpressionResolver) {
        this(timeExpressionResolver, new SearchFilterValidator());
    }

    public SearchIntentNormalizer(SearchTimeExpressionResolver timeExpressionResolver,
                                  SearchFilterValidator filterValidator) {
        this.timeExpressionResolver = timeExpressionResolver;
        this.filterValidator = filterValidator;
    }

    public SearchIntent normalize(SearchRequest request, SearchIntent input) {
        SearchIntent intent = input == null ? new SearchIntent() : input.copy();
        intent.setFilters(normalizedFilters(intent.getFilters()));
        filterValidator.validateAndNormalize(intent);
        removeFiltersOverriddenByRequest(request, intent);

        timeExpressionResolver.apply(request, intent);
        intent.setTextQuery(cleanTextQuery(intent.getTextQuery()));
        intent.setGroupBy(cleanField(intent.getGroupBy()));
        intent.setTimeBucket(cleanText(intent.getTimeBucket()));
        if ("quarter".equalsIgnoreCase(intent.getTimeBucket()) && !asksForQuarter(request.getQuestion())) {
            intent.setTimeBucket(null);
        }

        if (intent.getIntent() == null) {
            intent.setIntent(TemplateType.SIMPLE_SEARCH);
        }
        if (intent.getIntent() == TemplateType.TIME_AGGREGATION && !hasText(intent.getTimeBucket())) {
            boolean broadRange = hasText(intent.getTimeFrom());
            intent.setTimeBucket(timeExpressionResolver.inferBucket(intent.getSemanticSpans(), broadRange));
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
            if (hasText(value)) {
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

    private String canonicalize(String value) {
        String asciiD = value(value).replace('\u0111', 'd').replace('\u0110', 'D');
        String normalized = Normalizer.normalize(asciiD, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}+", "").toLowerCase(java.util.Locale.ROOT);
    }

    private String cleanField(String value) {
        if (!hasText(value)) {
            return null;
        }
        return canonicalize(value).replace(' ', '_');
    }

    private String cleanText(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private String cleanTextQuery(String value) {
        String cleaned = cleanText(value);
        if (cleaned == null) {
            return null;
        }
        String normalized = canonicalize(cleaned);
        if ("loi".equals(normalized) || "error".equals(normalized) || "errors".equals(normalized)) {
            return null;
        }
        return cleaned;
    }

    private boolean asksForQuarter(String question) {
        String normalized = canonicalize(question);
        return normalized.contains("theo quy")
                || normalized.contains("moi quy")
                || normalized.contains("by quarter")
                || normalized.contains("per quarter")
                || normalized.contains("quarterly");
    }

    private String value(String value) {
        return value == null ? "" : value;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
