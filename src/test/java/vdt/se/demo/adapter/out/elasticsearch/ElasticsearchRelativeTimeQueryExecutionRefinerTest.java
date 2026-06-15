package vdt.se.demo.adapter.out.elasticsearch;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.application.dto.QueryExecution;
import vdt.se.demo.application.dto.SearchRequest;
import vdt.se.demo.application.port.outboundPort.QueryExecutorPort;
import vdt.se.demo.domain.model.ExecutionResult;
import vdt.se.demo.domain.service.RelativeTimeWindowParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ElasticsearchRelativeTimeQueryExecutionRefinerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void rewritesEmptyRelativeNowWindowToLatestMatchingDataWindow() throws Exception {
        RelativeWindowExecutor executor = new RelativeWindowExecutor();
        ElasticsearchRelativeTimeQueryExecutionRefiner refiner =
                refiner(executor);
        SearchRequest request = new SearchRequest();
        request.setQuestion("Statistics on the number of login failed in the last 7 days");

        QueryExecution refined = refiner.refine(request, relativeWindowDsl(), new ExecutionResult(List.of(), List.of(), 0));

        assertThat(refined.executionResult().totalCount()).isEqualTo(1);
        assertThat(refined.generatedDsl().toString()).contains("2030-06-17T21:15:05Z");
        assertThat(refined.generatedDsl().toString()).contains("2030-06-24T21:15:05Z");
        assertThat(executor.executedDsl).hasSize(2);
    }

    @Test
    void rewritesLast24hToLatestMatchingDataWindow() throws Exception {
        RelativeWindowExecutor executor = new RelativeWindowExecutor();
        ElasticsearchRelativeTimeQueryExecutionRefiner refiner =
                refiner(executor);
        SearchRequest request = new SearchRequest();
        request.setQuestion("Failed login in last 24h");

        QueryExecution refined = refiner.refine(request, relativeWindowDsl(), new ExecutionResult(List.of(), List.of(), 0));

        assertThat(refined.executionResult().totalCount()).isEqualTo(1);
        assertThat(refined.generatedDsl().toString()).contains("2030-06-23T21:15:05Z");
        assertThat(refined.generatedDsl().toString()).contains("2030-06-24T21:15:05Z");
        assertThat(executor.executedDsl).hasSize(2);
    }

    @Test
    void keepsExplicitApiTimeBoundsEvenWhenResultIsEmpty() throws Exception {
        RelativeWindowExecutor executor = new RelativeWindowExecutor();
        ElasticsearchRelativeTimeQueryExecutionRefiner refiner =
                refiner(executor);
        SearchRequest request = new SearchRequest();
        request.setQuestion("Statistics on the number of login failed in the last 7 days");
        request.setFrom("2026-06-08T00:00:00Z");
        request.setTo("2026-06-15T00:00:00Z");
        ExecutionResult emptyResult = new ExecutionResult(List.of(), List.of(), 0);
        JsonNode originalDsl = relativeWindowDsl();

        QueryExecution refined = refiner.refine(request, originalDsl, emptyResult);

        assertThat(refined.generatedDsl()).isSameAs(originalDsl);
        assertThat(refined.executionResult()).isSameAs(emptyResult);
        assertThat(executor.executedDsl).isEmpty();
    }

    private JsonNode relativeWindowDsl() {
        return objectMapper.readTree("""
                {
                  "query": {
                    "bool": {
                      "must": [{"match_all": {}}],
                      "filter": [
                        {"term": {"event_type": "auth"}},
                        {"term": {"action": "failed"}},
                        {"range": {"timestamp": {"gte": "now-7d", "lte": "now"}}}
                      ]
                    }
                  },
                  "size": 0
                }
                """);
    }

    private ElasticsearchRelativeTimeQueryExecutionRefiner refiner(RelativeWindowExecutor executor) {
        return new ElasticsearchRelativeTimeQueryExecutionRefiner(
                executor,
                new RelativeTimeWindowParser(),
                new ElasticsearchDslTimeRangeEditor(),
                new ElasticsearchLatestTimestampResolver(executor, objectMapper)
        );
    }

    private static class RelativeWindowExecutor implements QueryExecutorPort {
        private final List<JsonNode> executedDsl = new ArrayList<>();

        @Override
        public ExecutionResult execute(JsonNode generatedDsl) {
            executedDsl.add(generatedDsl);
            if (generatedDsl.has("aggs")) {
                return new ExecutionResult(List.of(), List.of(Map.of(
                        "aggregation", "latest_timestamp",
                        "value", 1.908566105E12,
                        "value_as_string", "2030-06-24T21:15:05Z"
                )), 0);
            }
            String dsl = generatedDsl.toString();
            if (dsl.contains("2030-06-17T21:15:05Z") || dsl.contains("2030-06-23T21:15:05Z")) {
                return new ExecutionResult(List.of(Map.of("user", "alice")), List.of(), 1);
            }
            return new ExecutionResult(List.of(), List.of(), 0);
        }
    }
}
