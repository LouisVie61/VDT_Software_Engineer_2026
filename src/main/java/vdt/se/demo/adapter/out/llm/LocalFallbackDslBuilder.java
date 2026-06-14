package vdt.se.demo.adapter.out.llm;

import org.springframework.stereotype.Component;
import vdt.se.demo.application.dto.SearchRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class LocalFallbackDslBuilder {

    private static final String FULL_TEXT_SEARCH_FIELDS = """
            ["message","raw","user","host","source","event_type","severity","action"]
            """.trim();

    public String build(SearchRequest request) {
        String query = normalizeQuestion(request.getQuestion());
        List<String> filters = new ArrayList<>();
        addTermFilter(filters, "severity", request.getSeverity());
        addTermFilter(filters, "event_type", request.getEventType());
        addTermFilter(filters, "user", request.getUser());
        addTermFilter(filters, "host", request.getHost());
        addIpFilter(filters, request.getIp());
        addTimeRangeFilter(filters, request.getFrom(), request.getTo());

        int pageSize = Math.max(1, request.getPageSize());
        int fromOffset = Math.max(0, request.getPage()) * pageSize;
        return """
                {
                  "query": {
                    "bool": {
                      "must": [
                        {
                          "simple_query_string": {
                            "query": "%s",
                            "fields": %s,
                            "default_operator": "or"
                          }
                        }
                      ],
                      "filter": [%s]
                    }
                  },
                  "from": %d,
                  "size": %d,
                  "sort": [{"timestamp": {"order": "desc"}}]
                }
                """.formatted(escapeJson(query), FULL_TEXT_SEARCH_FIELDS, String.join(",", filters), fromOffset, pageSize);
    }

    private String normalizeQuestion(String question) {
        String normalized = question == null ? "" : question.toLowerCase(Locale.ROOT)
                .replaceAll("\\b(show|me|please|list|find|get|logs?|events?)\\b", " ")
                .replaceAll("[^a-z0-9._:/-]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
        if (normalized.contains("login") && !normalized.contains("auth")) {
            normalized = normalized + " auth";
        }
        return normalized.isBlank() ? "*" : normalized;
    }

    private void addTermFilter(List<String> filters, String field, String value) {
        if (value != null && !value.isBlank()) {
            filters.add("{\"term\":{\"%s\":\"%s\"}}".formatted(field, escapeJson(value.trim())));
        }
    }

    private void addIpFilter(List<String> filters, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        String escaped = escapeJson(value.trim());
        filters.add("""
                {"bool":{"should":[{"term":{"ip":"%s"}},{"term":{"src_ip":"%s"}},{"term":{"dst_ip":"%s"}}],"minimum_should_match":1}}
                """.formatted(escaped, escaped, escaped).trim());
    }

    private void addTimeRangeFilter(List<String> filters, String from, String to) {
        boolean hasFrom = from != null && !from.isBlank();
        boolean hasTo = to != null && !to.isBlank();
        if (!hasFrom && !hasTo) {
            return;
        }
        List<String> bounds = new ArrayList<>();
        if (hasFrom) {
            bounds.add("\"gte\":\"" + escapeJson(from.trim()) + "\"");
        }
        if (hasTo) {
            bounds.add("\"lte\":\"" + escapeJson(to.trim()) + "\"");
        }
        filters.add("{\"range\":{\"timestamp\":{" + String.join(",", bounds) + "}}}");
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
