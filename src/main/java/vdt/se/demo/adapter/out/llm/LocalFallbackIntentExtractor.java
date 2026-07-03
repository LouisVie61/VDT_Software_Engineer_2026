package vdt.se.demo.adapter.out.llm;

import org.springframework.stereotype.Component;
import vdt.se.demo.application.dto.SearchRequest;
import vdt.se.demo.domain.model.RoutingHint;
import vdt.se.demo.domain.model.SearchIntent;
import vdt.se.demo.domain.valueObjects.TemplateType;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class LocalFallbackIntentExtractor {
    private static final Set<String> SEVERITIES = Set.of("critical", "high", "medium", "low", "info");
    private static final Pattern TOP_N = Pattern.compile("\\btop\\s+(\\d{1,3})\\b");
    private static final Pattern YEAR = Pattern.compile("\\b((?:19|20)\\d{2})\\b");

    public SearchIntent extract(SearchRequest request, RoutingHint hint) {
        TemplateType type = inferType(request.getQuestion());
        SearchIntent intent = SearchIntent.builder()
                .intent(type)
                .textQuery(textQuery(request.getQuestion(), type))
                .filters(explicitFilters(request))
                .groupBy(type == TemplateType.TERMS_AGGREGATION ? groupBy(request.getQuestion()) : null)
                .metric(type == TemplateType.SIMPLE_SEARCH ? null : "COUNT")
                .topN(type == TemplateType.TERMS_AGGREGATION ? topN(request.getQuestion()) : null)
                .confidenceScores(confidenceScores(type))
                .build();
        if (type == TemplateType.TIME_AGGREGATION) {
            applyTimeAggregation(request.getQuestion(), intent);
        }
        return intent;
    }

    private TemplateType inferType(String question) {
        String text = normalize(question);
        if (isQuarterTrend(text) || containsAny(text, "over time", "timeline", "theo thoi gian",
                "theo gio", "theo ngay", "per hour", "per day")) {
            return TemplateType.TIME_AGGREGATION;
        }
        if (containsAny(text, "top", "count", "statistics", "stats", "statistic", "thong ke", "dem", "group by")) {
            return TemplateType.TERMS_AGGREGATION;
        }
        return TemplateType.SIMPLE_SEARCH;
    }

    private void applyTimeAggregation(String question, SearchIntent intent) {
        String text = normalize(question);
        if (isQuarterTrend(text)) {
            intent.setTimeBucket("quarter");
            Matcher matcher = YEAR.matcher(text);
            if (matcher.find()) {
                int year = Integer.parseInt(matcher.group(1));
                intent.setTimeFrom("%d-01-01T00:00:00Z".formatted(year));
                intent.setTimeTo("%d-01-01T00:00:00Z".formatted(year + 1));
            }
            return;
        }
        intent.setTimeBucket("1h");
    }

    private Map<String, String> explicitFilters(SearchRequest request) {
        Map<String, String> filters = new LinkedHashMap<>();
        put(filters, "severity", request.getSeverity());
        if (!filters.containsKey("severity")) {
            put(filters, "severity", severity(request.getQuestion()));
        }
        put(filters, "event_type", request.getEventType());
        if (!filters.containsKey("event_type") && looksLikeAuth(request.getQuestion())) {
            filters.put("event_type", "auth");
        }
        put(filters, "user", request.getUser());
        put(filters, "host", request.getHost());
        put(filters, "ip", request.getIp());
        return filters;
    }

    private String groupBy(String question) {
        String text = normalize(question);
        if (containsAny(text, "event type", "event_type", "loai event")) {
            return "event_type";
        }
        for (String field : new String[]{"severity", "action", "user", "host", "ip", "source"}) {
            if (containsAny(text, field, field.replace("_", " "))) {
                return field;
            }
        }
        return null;
    }

    private Integer topN(String question) {
        Matcher matcher = TOP_N.matcher(normalize(question));
        return matcher.find() ? Math.max(1, Math.min(100, Integer.parseInt(matcher.group(1)))) : null;
    }

    private String textQuery(String question, TemplateType type) {
        if (type != TemplateType.SIMPLE_SEARCH) {
            return null;
        }
        String text = normalize(question);
        if (looksLikeAuth(text)) {
            if (containsAny(text, "fail", "failed", "failure", "unsuccessful", "that bai")) {
                return "failed";
            }
            if (containsAny(text, "success", "successful", "succeeded", "thanh cong")) {
                return "success";
            }
            return null;
        }
        String normalized = text
                .replaceAll("\\b(show|me|please|list|find|get|logs?|events?|cho toi|hien thi|tim|lay)\\b", " ")
                .replaceAll("[^a-z0-9._:/-]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
        return normalized.isBlank() ? null : normalized;
    }

    private String severity(String question) {
        for (String token : normalize(question).split("\\s+")) {
            if (SEVERITIES.contains(token)) {
                return token;
            }
        }
        return null;
    }

    private Map<String, Double> confidenceScores(TemplateType type) {
        Map<String, Double> scores = new LinkedHashMap<>();
        scores.put("intent", type == TemplateType.SIMPLE_SEARCH ? 0.60d : 0.70d);
        scores.put("textQuery", 0.55d);
        scores.put("filters", 0.65d);
        scores.put("groupBy", 0.55d);
        scores.put("timeRange", type == TemplateType.TIME_AGGREGATION ? 0.70d : 0.50d);
        return scores;
    }

    private boolean isQuarterTrend(String text) {
        return containsAny(text, "theo quy", "by quarter", "per quarter", "quarterly");
    }

    private boolean looksLikeAuth(String value) {
        return containsAny(normalize(value), "auth", "login", "logon", "signin", "sign-in", "authentication", "dang nhap");
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

    private String normalize(String value) {
        String asciiD = (value == null ? "" : value).replace('\u0111', 'd').replace('\u0110', 'D');
        String normalized = Normalizer.normalize(asciiD, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
    }
}
