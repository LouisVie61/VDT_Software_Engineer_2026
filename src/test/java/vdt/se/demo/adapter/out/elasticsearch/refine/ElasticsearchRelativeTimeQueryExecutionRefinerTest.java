package vdt.se.demo.adapter.out.elasticsearch.refine;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.adapter.out.elasticsearch.dsl.ElasticsearchDslTimeRangeEditor;
import vdt.se.demo.adapter.out.elasticsearch.dsl.ElasticsearchExplicitFilterBuilder;
import vdt.se.demo.adapter.out.elasticsearch.dsl.ElasticsearchExplicitFilterDslEditor;
import vdt.se.demo.application.dto.QueryExecution;
import vdt.se.demo.application.dto.SearchRequest;
import vdt.se.demo.application.port.outboundPort.execution.QueryExecutorPort;
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
        SearchRequest request = SearchRequest.builder()
                .question("Statistics on the number of login failed in the last 7 days")
                .build();

        QueryExecution refined = refiner.refine(request, relativeWindowDsl(), executionResult(List.of(), List.of(), 0));

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
        SearchRequest request = SearchRequest.builder()
                .question("Failed login in last 24h")
                .build();

        QueryExecution refined = refiner.refine(request, relativeWindowDsl(), executionResult(List.of(), List.of(), 0));

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
        SearchRequest request = SearchRequest.builder()
                .question("Statistics on the number of login failed in the last 7 days")
                .from("2026-06-08T00:00:00Z")
                .to("2026-06-15T00:00:00Z")
                .build();
        ExecutionResult emptyResult = executionResult(List.of(), List.of(), 0);
        JsonNode originalDsl = relativeWindowDsl();

        QueryExecution refined = refiner.refine(request, originalDsl, emptyResult);

        assertThat(refined.generatedDsl()).isNotSameAs(originalDsl);
        assertThat(refined.generatedDsl().toString()).contains("2026-06-08T00:00:00Z");
        assertThat(refined.generatedDsl().toString()).contains("2026-06-15T00:00:00Z");
        assertThat(refined.generatedDsl().toString()).doesNotContain("now-7d");
        assertThat(refined.executionResult().totalCount()).isEqualTo(emptyResult.totalCount());
        assertThat(executor.executedDsl).singleElement().satisfies(executed ->
                assertThat(executed.toString()).isEqualTo(refined.generatedDsl().toString()));
    }

    @Test
    void appliesExplicitApiFiltersBeforeReturningExecution() throws Exception {
        RelativeWindowExecutor executor = new RelativeWindowExecutor();
        ElasticsearchRelativeTimeQueryExecutionRefiner refiner = refiner(executor);
        SearchRequest request = SearchRequest.builder()
                .question("show events")
                .from("2026-06-01T00:00:00Z")
                .to("2026-06-15T00:00:00Z")
                .severity("high")
                .eventType("auth")
                .user("alice")
                .host("host-1")
                .ip("10.0.0.1")
                .build();
        JsonNode generatedDsl = objectMapper.readTree("""
                {
                  "query": {"match_all": {}},
                  "size": 50
                }
                """);

        QueryExecution refined = refiner.refine(request, generatedDsl,
                executionResult(List.of(Map.of("unfiltered", true)), List.of(), 100));

        String dsl = refined.generatedDsl().toString();
        assertThat(refined.executionResult().totalCount()).isEqualTo(7);
        assertThat(dsl).contains("\"term\":{\"severity\":\"high\"}");
        assertThat(dsl).contains("\"term\":{\"event_type\":\"auth\"}");
        assertThat(dsl).contains("\"term\":{\"user\":\"alice\"}");
        assertThat(dsl).contains("\"term\":{\"host\":\"host-1\"}");
        assertThat(dsl).contains("\"term\":{\"ip\":\"10.0.0.1\"}");
        assertThat(dsl).contains("\"gte\":\"2026-06-01T00:00:00Z\"");
        assertThat(dsl).contains("\"lte\":\"2026-06-15T00:00:00Z\"");
        assertThat(executor.executedDsl).singleElement().satisfies(executed ->
                assertThat(executed.toString()).isEqualTo(dsl));
    }

    private JsonNode relativeWindowDsl() {
        return objectMapper.readTree("""
                {
                  "query": {
                    "bool": {
                      "must": [{"match_all": {}}],
                      "filter": [
                        {"term": {"event_type": "auth"}},
                        {"simple_query_string": {"query": "failed", "fields": ["message", "raw"]}},
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
                new ElasticsearchExplicitFilterDslEditor(objectMapper, new ElasticsearchExplicitFilterBuilder(objectMapper)),
                new RelativeTimeWindowParser(),
                new ElasticsearchDslTimeRangeEditor(),
                new ElasticsearchLatestTimestampResolver(executor, objectMapper)
        );
    }

    private static ExecutionResult executionResult(List<Map<String, Object>> results,
                                                   List<Map<String, Object>> aggregations,
                                                   int totalCount) {
        return ExecutionResult.builder()
                .results(results)
                .aggregations(aggregations)
                .totalCount(totalCount)
                .build();
    }

    private static class RelativeWindowExecutor implements QueryExecutorPort {
        private final List<JsonNode> executedDsl = new ArrayList<>();

        @Override
        public ExecutionResult execute(JsonNode generatedDsl) {
            executedDsl.add(generatedDsl);
            if (generatedDsl.has("aggs")) {
                return executionResult(List.of(), List.of(Map.of(
                        "aggregation", "latest_timestamp",
                        "value", 1.908566105E12,
                        "value_as_string", "2030-06-24T21:15:05Z"
                )), 0);
            }
            String dsl = generatedDsl.toString();
            if (dsl.contains("2030-06-17T21:15:05Z") || dsl.contains("2030-06-23T21:15:05Z")) {
                return executionResult(List.of(Map.of("user", "alice")), List.of(), 1);
            }
            if (dsl.contains("2026-06-01T00:00:00Z") && dsl.contains("10.0.0.1")) {
                return executionResult(List.of(Map.of("user", "alice")), List.of(), 7);
            }
            return executionResult(List.of(), List.of(), 0);
        }
    }
}
