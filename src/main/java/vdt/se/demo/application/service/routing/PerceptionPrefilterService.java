package vdt.se.demo.application.service.routing;

import vdt.se.demo.application.dto.SearchRequest;
import vdt.se.demo.application.service.template.GroupByResolver;
import vdt.se.demo.domain.model.RoutingHint;
import vdt.se.demo.domain.model.SearchIntent;
import vdt.se.demo.domain.valueObjects.TemplateType;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PerceptionPrefilterService {
    private static final Pattern TOP_N = Pattern.compile("\\btop\\s+(\\d{1,3})\\b");

    private final GroupByResolver groupByResolver;

    public PerceptionPrefilterService(GroupByResolver groupByResolver) {
        this.groupByResolver = groupByResolver;
    }

    public Optional<SearchIntent> prefilter(SearchRequest request, QueryRoutingService.RoutingDecision routing,
                                            boolean hasPreviousIntent) {
        if (hasPreviousIntent || request == null || routing == null) {
            return Optional.empty();
        }
        RoutingHint hint = routing.strongestSignal();
        if (hint == null || hint.neutral() || hint.confidence() < 0.74d || hint.templateType() != TemplateType.TERMS_AGGREGATION) {
            return Optional.empty();
        }
        String question = normalize(request.getQuestion());
        if (hasLinguisticAmbiguity(question)) {
            return Optional.empty();
        }
        Optional<String> groupBy = groupByResolver.resolve(question);
        if (groupBy.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(SearchIntent.builder()
                .intent(TemplateType.TERMS_AGGREGATION)
                .filters(explicitFilters(request))
                .groupBy(groupBy.get())
                .metric("COUNT")
                .topN(topN(question).orElse(null))
                .confidenceScores(Map.of(
                        "perceptionPrefilter", 0.82d,
                        "groupBy", 0.86d
                ))
                .build());
    }

    private boolean hasLinguisticAmbiguity(String question) {
        return containsAny(question,
                "last ", "past ", "previous ", "today", "yesterday", "hom nay", "tuan", "thang", "nam ",
                "202", "critical", "high", "medium", "low", "info", "login", "auth", "failed", "success",
                "malware", "attack", "tan cong", "xam nhap", "intrusion", "trong namh", "gan day", "vua qua", "gan nhat", "hom qua", 
                "tuan truoc", "thang truoc", "nam truoc");
    }

    private Map<String, String> explicitFilters(SearchRequest request) {
        Map<String, String> filters = new LinkedHashMap<>();
        put(filters, "severity", request.getSeverity());
        put(filters, "event_type", request.getEventType());
        put(filters, "user", request.getUser());
        put(filters, "host", request.getHost());
        put(filters, "ip", request.getIp());
        return filters;
    }

    private Optional<Integer> topN(String question) {
        Matcher matcher = TOP_N.matcher(question);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(Math.max(1, Math.min(100, Integer.parseInt(matcher.group(1)))));
    }

    private void put(Map<String, String> filters, String field, String value) {
        if (value != null && !value.isBlank()) {
            filters.put(field, value.trim());
        }
    }

    private boolean containsAny(String text, String... values) {
        for (String value : values) {
            if (text.contains(value)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        String asciiD = (value == null ? "" : value).replace('\u0111', 'd').replace('\u0110', 'D');
        String normalized = Normalizer.normalize(asciiD, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}+", "").toLowerCase(java.util.Locale.ROOT);
    }
}
