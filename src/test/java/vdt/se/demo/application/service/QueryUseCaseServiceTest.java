package vdt.se.demo.application.service;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.adapter.config.AppProperties;
import vdt.se.demo.application.dto.SearchRequest;
import vdt.se.demo.application.port.outboundPort.LlmFallbackChain;
import vdt.se.demo.application.port.outboundPort.LlmResponse;
import vdt.se.demo.application.port.outboundPort.QueryExecutorPort;
import vdt.se.demo.application.port.outboundPort.QueryHistoryPort;
import vdt.se.demo.domain.model.ExecutionResult;
import vdt.se.demo.domain.model.QueryHistory;
import vdt.se.demo.domain.model.QueryResult;
import vdt.se.demo.domain.service.ChartTypeInferenceService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class QueryUseCaseServiceTest {

    @Test
    void returnsGeneratedDslAndStoresHistory() {
        MemoryHistoryPort history = new MemoryHistoryPort();
        QueryUseCaseService service = new QueryUseCaseService(
                new StubLlm(),
                new StubExecutor(),
                history,
                auditLog -> {
                },
                new ChartTypeInferenceService(new ObjectMapper()),
                new ObjectMapper(),
                new AppProperties()
        );
        SearchRequest request = new SearchRequest();
        request.setQuestion("show failed login");

        QueryResult result = service.search(request);

        assertThat(result.getGeneratedDSL().has("query")).isTrue();
        assertThat(result.getSummary()).contains("summary");
        assertThat(history.rows).hasSize(1);
        assertThat(history.rows.getFirst().generatedDsl()).contains("\"match_all\"");
    }

    @Test
    void exportsHistorySnapshotAsCsv() {
        MemoryHistoryPort history = new MemoryHistoryPort();
        UUID id = UUID.randomUUID();
        history.rows.add(new QueryHistory(id, "soc-analyst-demo", "q", "{}", "s",
                vdt.se.demo.domain.valueObjects.ChartType.TABLE, 1,
                "[{\"user\":\"alice\",\"ip\":\"10.0.0.1\"}]", java.time.LocalDateTime.now()));
        QueryUseCaseService service = new QueryUseCaseService(new StubLlm(), new StubExecutor(), history,
                auditLog -> {
                }, new ChartTypeInferenceService(new ObjectMapper()), new ObjectMapper(), new AppProperties());

        String csv = service.exportCsv(id);

        assertThat(csv).contains("user,ip");
        assertThat(csv).contains("\"alice\",\"10.0.0.1\"");
    }

    private static class StubLlm implements LlmFallbackChain {
        private final ObjectMapper objectMapper = new ObjectMapper();

        @Override
        public LlmResponse generateDsl(SearchRequest request) {
            JsonNode dsl = objectMapper.readTree("{\"query\":{\"match_all\":{}},\"size\":50}");
            return new LlmResponse("TEST", dsl, dsl.toString());
        }

        @Override
        public String summarize(SearchRequest request, JsonNode generatedDsl, ExecutionResult executionResult) {
            return "summary text";
        }
    }

    private static class StubExecutor implements QueryExecutorPort {
        @Override
        public ExecutionResult execute(JsonNode generatedDsl) {
            return new ExecutionResult(List.of(Map.of("user", "alice")), List.of(), 1);
        }
    }

    private static class MemoryHistoryPort implements QueryHistoryPort {
        private final List<QueryHistory> rows = new ArrayList<>();

        @Override
        public void save(QueryHistory queryHistory) {
            rows.add(queryHistory);
        }

        @Override
        public List<QueryHistory> findRecent(String userIdentity, int limit) {
            return rows;
        }

        @Override
        public Optional<QueryHistory> findById(UUID id) {
            return rows.stream().filter(row -> row.id().equals(id)).findFirst();
        }
    }
}
