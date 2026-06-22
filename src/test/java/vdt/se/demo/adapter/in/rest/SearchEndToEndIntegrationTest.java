package vdt.se.demo.adapter.in.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.application.dto.IntentExtractionRequest;
import vdt.se.demo.application.port.outboundPort.audit.AuditLogPort;
import vdt.se.demo.application.port.outboundPort.cache.DslCachePort;
import vdt.se.demo.application.port.outboundPort.cache.IntentCachePort;
import vdt.se.demo.application.port.outboundPort.llm.IntentExtractionPort;
import vdt.se.demo.application.port.outboundPort.llm.SummaryPort;
import vdt.se.demo.application.port.outboundPort.execution.QueryExecutorPort;
import vdt.se.demo.application.port.outboundPort.history.QueryHistoryPort;
import vdt.se.demo.domain.model.CachedIntent;
import vdt.se.demo.domain.model.ExtractedIntent;
import vdt.se.demo.domain.model.ExecutionResult;
import vdt.se.demo.domain.model.PendingConfirmation;
import vdt.se.demo.domain.model.QueryHistory;
import vdt.se.demo.domain.model.SearchIntent;
import vdt.se.demo.domain.valueObjects.TemplateType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.lang.reflect.Proxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:search-e2e;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.elasticsearch.uris=http://localhost:9200",
        "app.elasticsearch.initialize-index=false"
})
@AutoConfigureMockMvc
class SearchEndToEndIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CapturingQueryExecutor queryExecutor;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void searchEndpointAppliesBasicFiltersAcrossTheApplicationFlow() throws Exception {
        queryExecutor.executedDsl.clear();
        String payload = """
                {
                  "question": "show events",
                  "page": 0,
                  "pageSize": 25,
                  "from": "2026-06-01T00:00:00Z",
                  "to": "2026-06-15T00:00:00Z",
                  "severity": "high",
                  "eventType": "auth",
                  "user": "alice",
                  "host": "host-1",
                  "ip": "10.0.0.1"
                }
                """;

        mockMvc.perform(post("/api/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.generatedDsl.query.bool.filter[0].term.severity").value("high"))
                .andExpect(jsonPath("$.generatedDsl.query.bool.filter[1].term.event_type").value("auth"))
                .andExpect(jsonPath("$.generatedDsl.query.bool.filter[2].term.user").value("alice"))
                .andExpect(jsonPath("$.generatedDsl.query.bool.filter[3].term.host").value("host-1"))
                .andExpect(jsonPath("$.generatedDsl.query.bool.filter[4].term.ip").value("10.0.0.1"))
                .andExpect(jsonPath("$.generatedDsl.query.bool.filter[5].range.timestamp.gte").value("2026-06-01T00:00:00Z"))
                .andExpect(jsonPath("$.generatedDsl.query.bool.filter[5].range.timestamp.lte").value("2026-06-15T00:00:00Z"));

        assertThat(queryExecutor.executedDsl).hasSize(1);
        assertThat(queryExecutor.executedDsl.getFirst().toString())
                .doesNotContain("\"match_all\"")
                .contains("\"severity\":\"high\"")
                .contains("\"event_type\":\"auth\"")
                .contains("\"user\":\"alice\"")
                .contains("\"host\":\"host-1\"")
                .contains("\"ip\":\"10.0.0.1\"");
    }

    @Test
    void searchEndpointTreatsNullPaginationAsDefaults() throws Exception {
        queryExecutor.executedDsl.clear();
        String payload = """
                {
                  "question": "show events",
                  "page": null,
                  "pageSize": null
                }
                """;

        mockMvc.perform(post("/api/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.pageSize").value(50));

        assertThat(queryExecutor.executedDsl).hasSize(1);
        assertThat(queryExecutor.executedDsl.getFirst().toString())
                .contains("\"size\":50")
                .doesNotContain("\"from\"");
    }

    @Test
    void aggregationSearchAlsoReturnsScrollableEventRowsAndExportsEventsCsv() throws Exception {
        queryExecutor.executedDsl.clear();
        String payload = """
                {
                  "question": "Top 10 IP co nhieu alert nhat",
                  "page": 0,
                  "pageSize": 500
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.needsConfirmation").value(false))
                .andExpect(jsonPath("$.totalCount").value(10000))
                .andExpect(jsonPath("$.aggregations.length()").value(10))
                .andExpect(jsonPath("$.results.length()").value(2))
                .andExpect(jsonPath("$.results[0].message").value("first event"))
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        String queryId = response.get("id").asString();
        mockMvc.perform(get("/api/search/{queryId}/export.csv", queryId))
                .andExpect(status().isOk())
                .andExpect(content -> assertThat(content.getResponse().getContentAsString())
                        .contains("message")
                        .contains("\"first event\"")
                        .doesNotContain("aggregation"));
    }

    @Test
    void aggregationWithoutExplicitTopNRequiresConfirmationBeforeCacheOrExecution() throws Exception {
        queryExecutor.executedDsl.clear();
        String payload = """
                {
                  "question": "Search ip duoc nhieu nhat nam 2021",
                  "page": 0,
                  "pageSize": 50
                }
                """;

        mockMvc.perform(post("/api/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.needsConfirmation").value(true))
                .andExpect(jsonPath("$.selectedTemplate").value("TERMS_AGGREGATION"))
                .andExpect(jsonPath("$.confirmation.confirmationId").isNotEmpty())
                .andExpect(jsonPath("$.confirmation.intent.topN").doesNotExist())
                .andExpect(jsonPath("$.confirmation.templateSelection.size").value(100))
                .andExpect(content -> assertThat(content.getResponse().getContentAsString())
                        .contains("TOP_N_REQUIRED"));

        assertThat(queryExecutor.executedDsl).isEmpty();
    }

    @TestConfiguration
    static class TestPorts {

        @Bean
        ElasticsearchOperations elasticsearchOperations() {
            return (ElasticsearchOperations) Proxy.newProxyInstance(
                    ElasticsearchOperations.class.getClassLoader(),
                    new Class<?>[]{ElasticsearchOperations.class},
                    (proxy, method, args) -> defaultValue(method.getReturnType()));
        }

        @Bean
        @Primary
        IntentExtractionPort intentExtractionPort() {
            return new StubIntentExtractionPort();
        }

        @Bean
        @Primary
        SummaryPort summaryPort() {
            return (request, generatedDsl, executionResult) -> "summary";
        }

        @Bean
        @Primary
        CapturingQueryExecutor queryExecutor() {
            return new CapturingQueryExecutor();
        }

        @Bean
        @Primary
        QueryHistoryPort queryHistoryPort() {
            return new MemoryQueryHistoryPort();
        }

        @Bean
        @Primary
        AuditLogPort auditLogPort() {
            return auditLog -> {
            };
        }

        @Bean
        @Primary
        DslCachePort dslCachePort() {
            return new MemoryDslCachePort();
        }

        @Bean
        @Primary
        IntentCachePort intentCachePort() {
            return new MemoryIntentCachePort();
        }

    }

    private static Object defaultValue(Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == char.class) {
            return '\0';
        }
        if (returnType == byte.class) {
            return (byte) 0;
        }
        if (returnType == short.class) {
            return (short) 0;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        if (returnType == float.class) {
            return 0f;
        }
        if (returnType == double.class) {
            return 0d;
        }
        return null;
    }

    private static class StubIntentExtractionPort implements IntentExtractionPort {
        @Override
        public ExtractedIntent extract(IntentExtractionRequest request) {
            SearchIntent intent = SearchIntent.builder()
                    .intent(request.routingHint().templateType() == null
                            ? TemplateType.SIMPLE_SEARCH
                            : request.routingHint().templateType())
                    .metric("COUNT")
                    .topN(request.request().getQuestion().contains("Top 10") ? 10 : null)
                    .build();
            return ExtractedIntent.builder()
                    .intent(intent)
                    .provider("TEST")
                    .rawContent("{}")
                    .build();
        }
    }

    static class CapturingQueryExecutor implements QueryExecutorPort {
        private final List<JsonNode> executedDsl = new ArrayList<>();

        @Override
        public ExecutionResult execute(JsonNode generatedDsl) {
            executedDsl.add(generatedDsl);
            if (generatedDsl.has("aggs")) {
                List<Map<String, Object>> buckets = new ArrayList<>();
                for (int i = 1; i <= 10; i++) {
                    buckets.add(Map.of("aggregation", "top_values", "key", "10.0.0." + i, "count", i));
                }
                return ExecutionResult.builder()
                        .results(List.of(
                                Map.of("id", "event-1", "message", "first event", "ip", "10.0.0.1"),
                                Map.of("id", "event-2", "message", "second event", "ip", "10.0.0.2")
                        ))
                        .aggregations(buckets)
                        .totalCount(10000)
                        .build();
            }
            boolean filtered = generatedDsl.toString().contains("\"severity\":\"high\"");
            if (filtered) {
                return ExecutionResult.builder()
                        .results(List.of(Map.of("user", "alice", "ip", "10.0.0.1")))
                        .aggregations(List.of())
                        .totalCount(1)
                        .build();
            }
            if (generatedDsl.path("size").asInt() >= 500) {
                return ExecutionResult.builder()
                        .results(List.of(
                        Map.of("id", "event-1", "message", "first event", "ip", "10.0.0.1"),
                        Map.of("id", "event-2", "message", "second event", "ip", "10.0.0.2")
                        ))
                        .aggregations(List.of())
                        .totalCount(10000)
                        .build();
            }
            return ExecutionResult.builder()
                    .results(List.of(Map.of("unfiltered", true)))
                    .aggregations(List.of())
                    .totalCount(99)
                    .build();
        }
    }

    private static class MemoryQueryHistoryPort implements QueryHistoryPort {
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
        public List<QueryHistory> findRecent(String userIdentity, String sessionId, int limit) {
            return rows;
        }

        @Override
        public Optional<QueryHistory> findById(UUID id) {
            return rows.stream().filter(row -> row.id().equals(id)).findFirst();
        }
    }

    private static class MemoryDslCachePort implements DslCachePort {
        private final Map<String, String> values = new HashMap<>();

        @Override
        public Optional<String> findFinalizedDsl(String schemaVersion, String sessionId, String queryHash) {
            return Optional.ofNullable(values.get(key(schemaVersion, sessionId, queryHash)));
        }

        @Override
        public void saveFinalizedDsl(String schemaVersion, String sessionId, String queryHash, String dsl) {
            values.put(key(schemaVersion, sessionId, queryHash), dsl);
        }

        private String key(String schemaVersion, String sessionId, String queryHash) {
            return schemaVersion + ":" + sessionId + ":" + queryHash;
        }
    }

    private static class MemoryIntentCachePort implements IntentCachePort {
        private final Map<String, PendingConfirmation> confirmations = new HashMap<>();

        @Override
        public Optional<CachedIntent> findLastClassifiedIntent(String sessionId) {
            return Optional.empty();
        }

        @Override
        public void saveLastClassifiedIntent(String sessionId, String schemaVersion, SearchIntent intent) {
        }

        @Override
        public void savePendingConfirmation(PendingConfirmation confirmation) {
            confirmations.put(confirmation.confirmationId(), confirmation);
        }

        @Override
        public Optional<PendingConfirmation> findPendingConfirmation(String confirmationId) {
            return Optional.ofNullable(confirmations.get(confirmationId));
        }
    }
}
