package vdt.se.demo.adapter.out.llm;

import org.springframework.stereotype.Component;
import vdt.se.demo.application.dto.SearchRequest;

import java.util.Set;

@Component
public class LlmDslPromptBuilder {

    public static final Set<String> FIELD_WHITELIST = Set.of(
            "timestamp", "source", "severity", "event_type", "user", "host", "ip",
            "message", "raw", "src_ip", "dst_ip", "action"
    );

    public String build(SearchRequest request) {
        return """
                You convert SOC analyst natural language questions in Vietnamese or English into Elasticsearch Query DSL.
                Return JSON only. Do not include markdown fences.

                Allowed fields only: timestamp, source, severity, event_type, user, host, ip, message, raw, src_ip, dst_ip, action.
                Use only Elasticsearch Query DSL, not SQL and not a wrapper object.
                Include all explicit API filters below directly in the DSL.
                Use page/from and pageSize/size from the request. Use timestamp:desc sort unless the question asks otherwise.
                Do not treat generic UI words like show, list, find, get, log, logs, event, events, or error as required search terms.
                Map SOC intent words to structured fields when possible:
                - login/authentication/sign-in means event_type=auth.
                - failed/failure means action=failed when the question is about authentication.
                Prefer structured filters for known SOC fields over literal full-text matching.

                Expected search DSL:
                {
                  "query": {
                    "bool": {
                      "must": [
                        {"simple_query_string": {"query": "failed login", "fields": ["message", "raw", "user", "host", "source", "event_type", "severity", "action"], "default_operator": "and"}}
                      ],
                      "filter": [
                        {"term": {"severity": "high"}},
                        {"range": {"timestamp": {"gte": "2026-01-01T00:00:00Z", "lte": "2026-01-02T00:00:00Z"}}}
                      ]
                    }
                  },
                  "from": 0,
                  "size": 50,
                  "sort": [{"timestamp": {"order": "desc"}}]
                }

                Expected login failure DSL:
                {
                  "query": {
                    "bool": {
                      "must": [{"match_all": {}}],
                      "filter": [
                        {"term": {"event_type": "auth"}},
                        {"term": {"action": "failed"}}
                      ]
                    }
                  },
                  "from": 0,
                  "size": 50,
                  "sort": [{"timestamp": {"order": "desc"}}]
                }

                Expected terms aggregation DSL:
                {
                  "query": {"bool": {"must": [{"match_all": {}}], "filter": []}},
                  "size": 0,
                  "aggs": {"top_values": {"terms": {"field": "ip", "size": 10}}}
                }

                Expected time aggregation DSL:
                {
                  "query": {"bool": {"must": [{"match_all": {}}], "filter": []}},
                  "size": 0,
                  "aggs": {"events_over_time": {"date_histogram": {"field": "timestamp", "fixed_interval": "1h"}}}
                }

                Question: %s
                Request pagination: page=%d, pageSize=%d, fromOffset=%d
                Explicit filters from API:
                from=%s, to=%s, severity=%s, event_type=%s, user=%s, host=%s, ip=%s
                """.formatted(
                request.getQuestion(),
                request.getPage(), request.getPageSize(), Math.max(0, request.getPage()) * Math.max(1, request.getPageSize()),
                value(request.getFrom()), value(request.getTo()), value(request.getSeverity()),
                value(request.getEventType()), value(request.getUser()), value(request.getHost()),
                value(request.getIp())
        );
    }

    private String value(String value) {
        return value == null || value.isBlank() ? "null" : value;
    }
}
