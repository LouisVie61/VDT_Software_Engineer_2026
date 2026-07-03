package vdt.se.demo.adapter.out.llm;

import org.springframework.stereotype.Component;
import vdt.se.demo.domain.model.ExecutionResult;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class DeterministicSummaryBuilder {

    public String build(ExecutionResult result) {
        if (result.aggregations().isEmpty() && result.results().isEmpty()) {
            return "Found " + result.totalCount() + " matching events. No result rows or aggregation buckets were returned in the current response. "
                    + "Review the generated query, relax restrictive filters, or widen the time window before continuing the investigation.";
        }

        StringBuilder summary = new StringBuilder("Found ")
                .append(result.totalCount())
                .append(" matching events");

        if (!result.aggregations().isEmpty()) {
            summary.append(" with ")
                    .append(result.aggregations().size())
                    .append(" aggregation buckets");
            topAggregation(result.aggregations()).ifPresent(top ->
                    summary.append("; the leading bucket is ").append(top));
        } else {
            summary.append("; the first page contains ")
                    .append(result.results().size())
                    .append(" returned rows");
        }
        summary.append(". ");

        List<String> highlights = List.of(
                topField(result.results(), "severity", "severity"),
                topField(result.results(), "event_type", "event type"),
                topField(result.results(), "user", "user"),
                topField(result.results(), "host", "host"),
                topField(result.results(), "ip", "IP"),
                topField(result.results(), "action", "action")
        ).stream().filter(value -> !value.isBlank()).limit(3).toList();

        if (highlights.isEmpty()) {
            summary.append("No dominant user, host, IP, severity, or event type is visible in the returned sample. ");
        } else {
            summary.append("Notable fields in the returned sample: ")
                    .append(String.join("; ", highlights))
                    .append(". ");
        }

        summary.append(nextStep(result));
        return summary.toString();
    }

    private java.util.Optional<String> topAggregation(List<Map<String, Object>> aggregations) {
        return aggregations.stream()
                .filter(row -> row.get("key") != null)
                .max(Comparator.comparingLong(row -> number(row.get("count"))))
                .map(row -> "'" + row.get("key") + "' (" + number(row.get("count")) + " events)");
    }

    private String topField(List<Map<String, Object>> rows, String field, String label) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Object value = row.get(field);
            if (value == null || String.valueOf(value).isBlank() || "-".equals(String.valueOf(value))) {
                continue;
            }
            counts.merge(String.valueOf(value), 1L, Long::sum);
        }
        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(entry -> label + "=" + entry.getKey() + " (" + entry.getValue() + " rows)")
                .orElse("");
    }

    private String nextStep(ExecutionResult result) {
        if (!result.aggregations().isEmpty()) {
            return "Next, drill into the highest bucket, inspect the matching raw events, and pivot by user, host, or IP to confirm scope.";
        }
        return "Next, inspect the highest-severity rows, open raw event details for repeated users/hosts/IPs, and narrow or bucket the time window if the result set is broad.";
    }

    private long number(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (RuntimeException ignored) {
            return 0L;
        }
    }
}
