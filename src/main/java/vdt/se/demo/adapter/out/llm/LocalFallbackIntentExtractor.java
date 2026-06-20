package vdt.se.demo.adapter.out.llm;

import org.springframework.stereotype.Component;
import vdt.se.demo.application.dto.SearchRequest;
import vdt.se.demo.domain.model.RoutingHint;
import vdt.se.demo.domain.model.SearchIntent;
import vdt.se.demo.domain.valueObjects.TemplateType;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class LocalFallbackIntentExtractor {
    private static final Set<String> SEVERITIES = Set.of("critical", "high", "medium", "low", "info");

    public SearchIntent extract(SearchRequest request, RoutingHint hint) {
        SearchIntent intent = SearchIntent.builder()
                .intent(infer(request.getQuestion()))
                .textQuery(normalizeQuestion(request.getQuestion()))
                .filters(explicitFilters(request))
                .groupBy(inferGroupBy(request.getQuestion()))
                .metric("COUNT")
                .topN(inferTopN(request.getQuestion()))
                .confidenceScores(confidenceScores())
                .build();
        if (intent.getIntent() == TemplateType.TIME_AGGREGATION) {
            intent.setTimeBucket("1h");
        }
        return intent;
    }

    private TemplateType infer(String question) {
        String text = lower(question);
        if (text.contains("over time") || text.contains("timeline") || text.contains("theo thời gian")) {
            return TemplateType.TIME_AGGREGATION;
        }
        if (text.contains("top") || text.contains("count") || text.contains("đếm") || text.contains("theo")) {
            return TemplateType.TERMS_AGGREGATION;
        }
        return TemplateType.SIMPLE_SEARCH;
    }

    private Map<String, String> explicitFilters(SearchRequest request) {
        Map<String, String> filters = new LinkedHashMap<>();
        put(filters, "severity", request.getSeverity());
        if (!filters.containsKey("severity")) {
            put(filters, "severity", inferSeverity(request.getQuestion()));
        }
        put(filters, "event_type", request.getEventType());
        if (!filters.containsKey("event_type") && looksLikeAuthSearch(request.getQuestion())) {
            filters.put("event_type", "auth");
        }
        put(filters, "user", request.getUser());
        put(filters, "host", request.getHost());
        put(filters, "ip", request.getIp());
        return filters;
    }

    private String inferGroupBy(String question) {
        String text = lower(question);
        if (!isAggregationQuestion(text)) {
            return null;
        }
        if (containsAny(text, "loi", "error", "failure")) {
            return "event_type";
        }
        for (String field : new String[]{"severity", "event_type", "action", "user", "host", "ip", "source", "message", "raw"}) {
            if (text.contains(field) || text.contains(field.replace("_", " "))) {
                return field;
            }
        }
        return null;
    }

    private Integer inferTopN(String question) {
        String text = lower(question);
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\btop\\s+(\\d+)\\b").matcher(text);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : null;
    }

    private Map<String, Double> confidenceScores() {
        Map<String, Double> scores = new LinkedHashMap<>();
        scores.put("textQuery", 0.70d);
        scores.put("filters", 0.70d);
        scores.put("groupBy", 0.65d);
        scores.put("timeRange", 0.65d);
        return scores;
    }

    private String inferSeverity(String question) {
        for (String token : lower(question).split("\\s+")) {
            String normalized = token.replaceAll("[^a-z]", "");
            if (SEVERITIES.contains(normalized)) {
                return normalized;
            }
        }
        return null;
    }

    private String normalizeQuestion(String question) {
        String text = lower(question);
        if (looksLikeAuthSearch(text)) {
            if (containsAny(text, "fail", "failed", "failure", "unsuccessful")) {
                return "failed";
            }
            if (containsAny(text, "success", "successful", "succeeded")) {
                return "success";
            }
            if (containsAny(text, "lock", "locked", "lockout")) {
                return "locked";
            }
            if (containsAny(text, "bypass")) {
                return "bypass";
            }
            return "*";
        }
        String normalized = lower(question)
                .replaceAll("\\b(show|me|please|list|find|get|logs?|events?)\\b", " ")
                .replaceAll("[^a-z0-9._:/-]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
        return normalized.isBlank() ? "*" : normalized;
    }

    private boolean looksLikeAuthSearch(String text) {
        return containsAny(lower(text), "auth", "login", "logon", "signin", "sign-in", "authentication");
    }

    private boolean isAggregationQuestion(String text) {
        return containsAny(text, "top", "count", "statistics", "stats", "group by", "by ", "theo", "dem", "thong ke");
    }

    private boolean containsAny(String text, String... needles) {
        if (text == null || text.isBlank()) {
            return false;
        }
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private void put(Map<String, String> filters, String field, String value) {
        if (value != null && !value.isBlank()) {
            filters.put(field, value.trim());
        }
    }

    private String lower(String value) {
        String asciiD = (value == null ? "" : value).replace('đ', 'd').replace('Đ', 'D');
        String normalized = java.text.Normalizer.normalize(asciiD, java.text.Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
    }
}

