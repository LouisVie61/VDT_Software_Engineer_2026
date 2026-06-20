package vdt.se.demo.adapter.out.llm;

import org.junit.jupiter.api.Test;
import vdt.se.demo.domain.model.ExecutionResult;

import java.util.List;

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
    }
}
