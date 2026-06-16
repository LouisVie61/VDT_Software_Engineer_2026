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
import vdt.se.demo.application.dto.SearchRequest;
import vdt.se.demo.application.port.outboundPort.AuditLogPort;
import vdt.se.demo.application.port.outboundPort.EventIndexPort;
import vdt.se.demo.application.port.outboundPort.LlmFallbackChain;
import vdt.se.demo.application.port.outboundPort.LlmResponse;
import vdt.se.demo.application.port.outboundPort.QueryExecutorPort;
import vdt.se.demo.application.port.outboundPort.QueryHistoryPort;
import vdt.se.demo.domain.model.ExecutionResult;
import vdt.se.demo.domain.model.QueryHistory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:search-e2e;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.elasticsearch.uris=http://localhost:9200"
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
                .andExpect(jsonPath("$.generatedDsl.query.bool.filter[4].bool.minimum_should_match").value(1))
                .andExpect(jsonPath("$.generatedDsl.query.bool.filter[5].range.timestamp.gte").value("2026-06-01T00:00:00Z"))
                .andExpect(jsonPath("$.generatedDsl.query.bool.filter[5].range.timestamp.lte").value("2026-06-15T00:00:00Z"));

        assertThat(queryExecutor.executedDsl).hasSize(2);
        assertThat(queryExecutor.executedDsl.getFirst().toString()).contains("\"match_all\"");
        assertThat(queryExecutor.executedDsl.getLast().toString())
                .contains("\"severity\":\"high\"")
                .contains("\"event_type\":\"auth\"")
                .contains("\"user\":\"alice\"")
                .contains("\"host\":\"host-1\"")
                .contains("\"ip\":\"10.0.0.1\"")
                .contains("\"src_ip\":\"10.0.0.1\"")
                .contains("\"dst_ip\":\"10.0.0.1\"");
    }

    @Test
    void aggregationSearchAlsoReturnsScrollableEventRowsAndExportsEventsCsv() throws Exception {
        queryExecutor.executedDsl.clear();
        String payload = """
                {
                  "question": "Top 10 IP co nhieu alert nhat",
                  "page": 0,
                  "pageSize": 10000
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
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

    @TestConfiguration
    static class TestPorts {

        @Bean
        ElasticsearchOperations elasticsearchOperations() {
            return mock(ElasticsearchOperations.class);
        }

        @Bean
        @Primary
        LlmFallbackChain llmFallbackChain(ObjectMapper objectMapper) {
            return new StubLlmFallbackChain(objectMapper);
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
        EventIndexPort eventIndexPort() {
            return new EventIndexPort() {
                @Override
                public void ensureIndex() {
                }

                @Override
                public void indexBatch(List<vdt.se.demo.domain.model.SocEvent> events) {
                }

                @Override
                public String indexName() {
                    return "test-events";
                }
            };
        }
    }

    private static class StubLlmFallbackChain implements LlmFallbackChain {
        private final ObjectMapper objectMapper;

        private StubLlmFallbackChain(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public LlmResponse generateDsl(SearchRequest request) {
            if (request.getQuestion().contains("Top 10 IP")) {
                JsonNode dsl = objectMapper.readTree("""
                        {
                          "query": {"match_all": {}},
                          "size": 0,
                          "aggs": {
                            "top_values": {
                              "terms": {"field": "ip", "size": 10}
                            }
                          }
                        }
                        """);
                return new LlmResponse("TEST", dsl, dsl.toString());
            }
            JsonNode dsl = objectMapper.readTree("""
                    {
                      "query": {"match_all": {}},
                      "from": 0,
                      "size": 25
                    }
                    """);
            return new LlmResponse("TEST", dsl, dsl.toString());
        }

        @Override
        public String summarize(SearchRequest request, JsonNode generatedDsl, ExecutionResult executionResult) {
            return "summary";
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
                return new ExecutionResult(List.of(), buckets, 10000);
            }
            boolean filtered = generatedDsl.toString().contains("\"severity\":\"high\"");
            if (filtered) {
                return new ExecutionResult(List.of(Map.of("user", "alice", "ip", "10.0.0.1")), List.of(), 1);
            }
            if (generatedDsl.path("size").asInt() == 10000) {
                return new ExecutionResult(List.of(
                        Map.of("id", "event-1", "message", "first event", "ip", "10.0.0.1"),
                        Map.of("id", "event-2", "message", "second event", "ip", "10.0.0.2")
                ), List.of(), 10000);
            }
            return new ExecutionResult(List.of(Map.of("unfiltered", true)), List.of(), 99);
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
        public Optional<QueryHistory> findById(UUID id) {
            return rows.stream().filter(row -> row.id().equals(id)).findFirst();
        }
    }
}
