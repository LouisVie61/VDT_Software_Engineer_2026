package vdt.se.demo.application.service.query;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.domain.iql.IqlQuery;
import vdt.se.demo.domain.model.ExecutionResult;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ResultSummaryBuilderTest {
    @Test
    void retainsReferenceableBucketKeysAndCapsSessionProjection() {
        IqlQuery query = new IqlQuery(List.of(), List.of(), null, null,
                List.of(new IqlQuery.GroupBy("host", 100)), List.of(), null, List.of(), 50, null);
        List<Map<String, Object>> rows = java.util.stream.IntStream.range(0, 25)
                .mapToObj(index -> Map.<String, Object>of("key", "host-" + index, "count", 25 - index)).toList();

        var summary = new ResultSummaryBuilder(new ObjectMapper())
                .build(query, new ExecutionResult(List.of(), rows, 100));

        assertThat(summary.buckets()).hasSize(20);
        assertThat(summary.buckets().getFirst().key().get("host").asString()).isEqualTo("host-0");
        assertThat(summary.buckets().getFirst().metrics().get("count")).isEqualTo(25);
    }
}
