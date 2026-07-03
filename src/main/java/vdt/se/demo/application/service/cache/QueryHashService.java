package vdt.se.demo.application.service.cache;

import vdt.se.demo.application.dto.SearchRequest;
import vdt.se.demo.domain.model.CanonicalQueryPlan;
import vdt.se.demo.domain.model.SearchIntent;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import java.util.TreeMap;

public class QueryHashService {
    public String hash(String question) {
        return digest(normalize(question));
    }

    public String hash(SearchRequest request) {
        String normalized = String.join("|",
                normalize(request == null ? null : request.getQuestion()),
                normalize(request == null ? null : request.getFrom()),
                normalize(request == null ? null : request.getTo()),
                normalize(request == null ? null : request.getSeverity()),
                normalize(request == null ? null : request.getEventType()),
                normalize(request == null ? null : request.getUser()),
                normalize(request == null ? null : request.getHost()),
                normalize(request == null ? null : request.getIp()),
                normalize(request == null ? null : request.getSessionId()),
                normalize(request == null ? null : request.getSearchAfter()),
                String.valueOf(request == null ? 0 : request.getPage()),
                String.valueOf(request == null ? 50 : request.getPageSize()));
        return digest(normalized);
    }

    public String hash(SearchRequest request, CanonicalQueryPlan plan) {
        SearchIntent intent = plan == null ? null : plan.mergedIntent();
        String normalized = String.join("|",
                normalize(request == null ? null : request.getQuestion()),
                normalize(request == null ? null : request.getFrom()),
                normalize(request == null ? null : request.getTo()),
                normalize(request == null ? null : request.getSeverity()),
                normalize(request == null ? null : request.getEventType()),
                normalize(request == null ? null : request.getUser()),
                normalize(request == null ? null : request.getHost()),
                normalize(request == null ? null : request.getIp()),
                normalize(request == null ? null : request.getSessionId()),
                normalize(request == null ? null : request.getSearchAfter()),
                String.valueOf(request == null ? 0 : request.getPage()),
                String.valueOf(request == null ? 50 : request.getPageSize()),
                plan == null ? "" : normalize(plan.normalizedQuery()),
                plan == null ? "" : normalize(plan.sessionId()),
                plan == null || plan.templateSelection() == null ? "" : plan.templateSelection().type().name(),
                plan == null || plan.templateSelection() == null ? "" : normalize(plan.templateSelection().groupBy()),
                plan == null || plan.templateSelection() == null ? "" : String.valueOf(plan.templateSelection().size()),
                normalize(intent == null ? null : intent.getTextQuery()),
                normalize(intent == null ? null : intent.getTimeFrom()),
                normalize(intent == null ? null : intent.getTimeTo()),
                normalize(intent == null ? null : intent.getTimeBucket()),
                filters(intent));
        return digest(normalized);
    }

    private String normalize(String value) {
        return value == null ? "" : value.strip().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private String filters(SearchIntent intent) {
        if (intent == null || intent.getFilters() == null || intent.getFilters().isEmpty()) {
            return "";
        }
        return new TreeMap<>(intent.getFilters()).toString();
    }

    private String digest(String normalized) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(normalized.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Cannot hash query", e);
        }
    }
}

