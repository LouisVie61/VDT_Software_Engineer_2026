package vdt.se.demo.adapter.out.llm;

import org.junit.jupiter.api.Test;
import vdt.se.demo.domain.model.ExecutionResult;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DeterministicSummaryBuilderTest {

    @Test
    void includesTotalCount() {
        String summary = new DeterministicSummaryBuilder().build(ExecutionResult.builder()
                .results(List.of())
                .aggregations(List.of())
                .totalCount(7)
                .build());

        assertThat(summary).contains("Found 7 matching events");
        assertThat(summary).contains("widen the time window");
    }

    @Test
    void highlightsReturnedFieldsAndNextInvestigationStep() {
        String summary = new DeterministicSummaryBuilder().build(ExecutionResult.builder()
                .results(List.of(
                        Map.of("severity", "high", "event_type", "auth", "user", "alice", "host", "host-1", "ip", "10.0.0.1"),
                        Map.of("severity", "high", "event_type", "auth", "user", "alice", "host", "host-2", "ip", "10.0.0.2")
                ))
                .aggregations(List.of())
                .totalCount(2)
                .build());

        assertThat(summary).contains("severity=high");
        assertThat(summary).contains("event type=auth");
        assertThat(summary).contains("user=alice");
        assertThat(summary).contains("Next, inspect");
    }
}
