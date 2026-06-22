package vdt.se.demo.adapter.out.llm;

import org.springframework.stereotype.Component;
import vdt.se.demo.application.dto.SearchRequest;

import java.time.Instant;
import java.util.Set;

@Component
public class LlmDslPromptBuilder {

    public static final Set<String> FIELD_WHITELIST = Set.of(
            "timestamp", "source", "severity", "event_type", "user", "host", "ip",
            "message", "raw"
    );

    private static final String SYSTEM_CONTRACT = """
            You are a SOC analyst assistant. Convert the analyst's natural language question
            into a valid Elasticsearch Query DSL JSON object.

            Return raw JSON only. No markdown fences. No explanations. No wrapper object.
            If the query cannot be resolved, return:
            {"error": "unresolvable", "reason": "<brief reason>"}

            The application canonical plan is authoritative when available.
            Perception/routing output is only a soft prior and must never force an incorrect DSL shape.
            """;

    private static final String SCHEMA_CONTRACT = """
            Current datetime:
            Now: %s (ISO 8601, UTC)
            Use this to resolve all relative and calendar time expressions to absolute ISO 8601 timestamps.
            Never use relative strings in DSL.
            Quarter 1 = Jan 01 to Mar 31.
            Quarter 2 = Apr 01 to Jun 30.
            Quarter 3 = Jul 01 to Sep 30.
            Quarter 4 = Oct 01 to Dec 31.
            "tuan truoc" = past 7 days from now.
            "hom nay" = start of today UTC to now.
            "thang nay" = first day of current month to now.
            "dau nam" = January 1 of current year to now.

            Allowed fields:
            - timestamp: date, range filters and date_histogram only
            - source: keyword
            - severity: keyword
            - event_type: keyword
            - user: keyword
            - host: keyword
            - ip: ip
            - message: text, free-text search only
            - raw: not indexed, never query/filter/aggregate

            Use bool.filter for exact filters and ranges.
            Use simple_query_string on ["message"] only for unresolved free text.
            Search DSL must include from, size, and timestamp desc sort.
            Aggregation DSL must set size=0 and use aggs.result.
            Terms aggregation fields: source, severity, event_type, user, host, ip.
            Date histogram field: timestamp.

            Known field values:
            severity values: critical, high, medium, low, info
            event_type examples: auth, failed_login, malware, network, process, file, alert
            Use exact values only for term and terms filters.
            If the user's description maps to a known event_type exactly, use an event_type term filter.
            Only search message when the description is ambiguous or cannot map to any known field value.

            Intent word mapping:
            UI intent words are not search content: show, list, find, get, log, logs, event, events,
            error, errors, display, give, tell, cho toi, hien thi, tim, lay.
            login / authentication / sign-in / dang nhap maps to event_type when possible.
            failed / failure / that bai / loi dang nhap maps to message free text when not a known event_type.
            statistics / thong ke / dem / count / top maps to aggregation.
            trend / xu huong / theo thoi gian / over time / per hour / per day maps to date_histogram.
            """;

    public String build(SearchRequest request) {
        int page = Math.max(0, request.getPage());
        int pageSize = Math.max(1, request.getPageSize());
        return """
                %s

                %s

                Expected search DSL:
                {
                  "query": {"bool": {"filter": [], "must": []}},
                  "from": %d,
                  "size": %d,
                  "sort": [{"timestamp": {"order": "desc"}}]
                }

                Expected terms aggregation DSL:
                {
                  "query": {"bool": {"filter": []}},
                  "size": 0,
                  "aggs": {"result": {"terms": {"field": "<groupable field>", "size": 10}}}
                }

                Request:
                Question: %s
                Page: %d
                Page size: %d
                From offset: %d

                Explicit API filters. Omit null or blank values:
                %s
                """.formatted(
                SYSTEM_CONTRACT,
                SCHEMA_CONTRACT.formatted(Instant.now().toString()),
                page * pageSize,
                pageSize,
                value(request.getQuestion()),
                page,
                pageSize,
                page * pageSize,
                filterBlock(request)
        );
    }

    private String filterBlock(SearchRequest request) {
        return """
                from=%s
                to=%s
                severity=%s
                event_type=%s
                user=%s
                host=%s
                ip=%s
                """.formatted(
                value(request.getFrom()),
                value(request.getTo()),
                value(request.getSeverity()),
                value(request.getEventType()),
                value(request.getUser()),
                value(request.getHost()),
                value(request.getIp())
        );
    }

    private String value(String value) {
        return value == null || value.isBlank() ? "null" : value;
    }
}
