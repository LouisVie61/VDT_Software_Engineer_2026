package vdt.se.demo.application.service;

import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.adapter.config.AppProperties;
import vdt.se.demo.application.dto.SearchRequest;
import vdt.se.demo.application.port.inboundPort.QueryUseCase;
import vdt.se.demo.application.port.outboundPort.AuditLogPort;
import vdt.se.demo.application.port.outboundPort.LlmFallbackChain;
import vdt.se.demo.application.port.outboundPort.LlmResponse;
import vdt.se.demo.application.port.outboundPort.QueryExecutorPort;
import vdt.se.demo.application.port.outboundPort.QueryHistoryPort;
import vdt.se.demo.domain.exception.BadQueryException;
import vdt.se.demo.domain.model.AuditLog;
import vdt.se.demo.domain.model.ExecutionResult;
import vdt.se.demo.domain.model.QueryHistory;
import vdt.se.demo.domain.model.QueryResult;
import vdt.se.demo.domain.service.ChartTypeInferenceService;
import vdt.se.demo.domain.valueObjects.AuditStatus;
import vdt.se.demo.domain.valueObjects.ChartType;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class QueryUseCaseService implements QueryUseCase {

    private final LlmFallbackChain llmFallbackChain;
    private final QueryExecutorPort queryExecutorPort;
    private final QueryHistoryPort queryHistoryPort;
    private final AuditLogPort auditLogPort;
    private final ChartTypeInferenceService chartTypeInferenceService;
    private final ObjectMapper objectMapper;
    private final AppProperties properties;

    public QueryUseCaseService(LlmFallbackChain llmFallbackChain, QueryExecutorPort queryExecutorPort,
                               QueryHistoryPort queryHistoryPort, AuditLogPort auditLogPort,
                               ChartTypeInferenceService chartTypeInferenceService, ObjectMapper objectMapper,
                               AppProperties properties) {
        this.llmFallbackChain = llmFallbackChain;
        this.queryExecutorPort = queryExecutorPort;
        this.queryHistoryPort = queryHistoryPort;
        this.auditLogPort = auditLogPort;
        this.chartTypeInferenceService = chartTypeInferenceService;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public QueryResult search(SearchRequest request) {
        UUID queryId = UUID.randomUUID();
        LocalDateTime started = LocalDateTime.now();
        String generatedDsl = null;
        String provider = null;

        try {
            LlmResponse llmResponse = llmFallbackChain.generateDsl(request);
            provider = llmResponse.provider();
            JsonNode generatedDslNode = llmResponse.generatedDsl();
            generatedDsl = objectMapper.writeValueAsString(generatedDslNode);
            ExecutionResult executionResult = queryExecutorPort.execute(generatedDslNode);
            String summary = llmFallbackChain.summarize(request, generatedDslNode, executionResult);
            ChartType chartType = chartTypeInferenceService.inferChartType(generatedDslNode);

            QueryResult result = new QueryResult(
                    queryId,
                    request.getQuestion(),
                    generatedDslNode,
                    summary,
                    executionResult.results(),
                    executionResult.aggregations(),
                    executionResult.totalCount(),
                    chartType,
                    request.getPage(),
                    request.getPageSize()
            );

            queryHistoryPort.save(new QueryHistory(
                    queryId,
                    properties.getUser().getDefaultId(),
                    request.getQuestion(),
                    generatedDsl,
                    summary,
                    chartType,
                    executionResult.totalCount(),
                    objectMapper.writeValueAsString(executionResult.results()),
                    LocalDateTime.now()
            ));

            auditLogPort.saveAsync(audit(
                    queryId, request, generatedDsl, executionResult.totalCount(),
                    Duration.between(started, LocalDateTime.now()).toMillis(), AuditStatus.SUCCESS, provider, null
            ));
            return result;
        } catch (RuntimeException e) {
            auditLogPort.saveAsync(audit(
                    queryId, request, generatedDsl, null,
                    Duration.between(started, LocalDateTime.now()).toMillis(), AuditStatus.FAILED, provider, e.getMessage()
            ));
            throw e;
        } catch (Exception e) {
            auditLogPort.saveAsync(audit(
                    queryId, request, generatedDsl, null,
                    Duration.between(started, LocalDateTime.now()).toMillis(), AuditStatus.FAILED, provider, e.getMessage()
            ));
            throw new BadQueryException("Search failed", e);
        }
    }

    @Override
    public List<QueryHistory> history(String userIdentity, int limit) {
        String user = userIdentity == null || userIdentity.isBlank() ? properties.getUser().getDefaultId() : userIdentity;
        return queryHistoryPort.findRecent(user, limit);
    }

    @Override
    public String exportCsv(UUID queryId) {
        QueryHistory history = queryHistoryPort.findById(queryId)
                .orElseThrow(() -> new BadQueryException("Query history not found: " + queryId));
        try {
            JsonNode rows = objectMapper.readTree(history.resultSnapshot());
            if (!rows.isArray() || rows.isEmpty()) {
                return "";
            }

            Set<String> headers = new LinkedHashSet<>();
            for (JsonNode row : rows) {
                for (Map.Entry<String, JsonNode> field : row.properties()) {
                    headers.add(field.getKey());
                }
            }

            StringBuilder csv = new StringBuilder();
            csv.append(String.join(",", headers)).append("\n");
            for (JsonNode row : rows) {
                boolean first = true;
                for (String header : headers) {
                    if (!first) {
                        csv.append(",");
                    }
                    first = false;
                    JsonNode value = row.get(header);
                    csv.append(escapeCsv(value == null || value.isNull() ? "" : value.asString()));
                }
                csv.append("\n");
            }
            return csv.toString();
        } catch (Exception e) {
            throw new BadQueryException("Cannot export query result as CSV", e);
        }
    }

    private AuditLog audit(UUID id, SearchRequest request, String dsl, Integer count, Long durationMs,
                           AuditStatus status, String provider, String errorMessage) {
        return new AuditLog(
                id,
                properties.getUser().getDefaultId(),
                LocalDateTime.now(),
                request.getQuestion(),
                dsl,
                count,
                durationMs,
                status,
                provider,
                errorMessage
        );
    }

    private String escapeCsv(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
