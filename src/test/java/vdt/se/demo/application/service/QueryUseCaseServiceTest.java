package vdt.se.demo.application.service;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.adapter.config.AppProperties;
import vdt.se.demo.application.dto.QueryExecution;
import vdt.se.demo.application.dto.SearchRequest;
import vdt.se.demo.application.port.outboundPort.audit.AuditLogPort;
import vdt.se.demo.application.dto.IntentExtractionRequest;
import vdt.se.demo.application.port.outboundPort.cache.DslCachePort;
import vdt.se.demo.application.port.outboundPort.cache.IntentCachePort;
import vdt.se.demo.application.port.outboundPort.execution.QueryExecutionRefiner;
import vdt.se.demo.application.port.outboundPort.execution.QueryExecutorPort;
import vdt.se.demo.application.port.outboundPort.execution.SearchDslBuilderPort;
import vdt.se.demo.application.port.outboundPort.history.QueryHistoryPort;
import vdt.se.demo.application.port.outboundPort.llm.IntentExtractionPort;
import vdt.se.demo.application.port.outboundPort.llm.SummaryPort;
import vdt.se.demo.application.service.cache.QueryHashService;
import vdt.se.demo.application.service.context.ContextRetrievalService;
import vdt.se.demo.application.service.execution.SearchExecutionService;
import vdt.se.demo.application.service.intent.IntentMergeService;
import vdt.se.demo.application.service.intent.SearchIntentNormalizer;
import vdt.se.demo.application.service.query.FinalizedSearchService;
import vdt.se.demo.application.service.query.DiagnosticDslVariantFactory;
import vdt.se.demo.application.service.query.PendingConfirmationResultFactory;
import vdt.se.demo.application.service.query.QueryAuditService;
import vdt.se.demo.application.service.query.QueryConfirmationWorkflow;
import vdt.se.demo.application.service.query.QueryCsvExportService;
import vdt.se.demo.application.service.query.QueryDiagnosticService;
import vdt.se.demo.application.service.query.QueryResultPersistenceService;
import vdt.se.demo.application.service.query.QuerySearchWorkflow;
import vdt.se.demo.application.service.query.QuerySummaryService;
import vdt.se.demo.application.service.query.QueryUseCaseService;
import vdt.se.demo.application.service.query.SearchCacheContextService;
import vdt.se.demo.application.service.query.SearchDslCacheLookupService;
import vdt.se.demo.application.service.query.SearchPlanPreparationService;
import vdt.se.demo.application.service.query.ZeroResultDiagnosticClassifier;
import vdt.se.demo.application.service.routing.QueryRoutingService;
import vdt.se.demo.application.service.template.CanonicalPlanBuilder;
import vdt.se.demo.application.service.template.GroupByResolver;
import vdt.se.demo.application.service.template.TemplateSelectionService;
import vdt.se.demo.application.service.template.TemplateIntentSelector;
import vdt.se.demo.domain.model.CanonicalQueryPlan;
import vdt.se.demo.domain.model.CachedIntent;
import vdt.se.demo.domain.model.ExtractedIntent;
import vdt.se.demo.domain.model.ExecutionResult;
import vdt.se.demo.domain.model.PendingConfirmation;
import vdt.se.demo.domain.model.QueryHistory;
import vdt.se.demo.domain.model.QueryResult;
import vdt.se.demo.domain.model.SearchIntent;
import vdt.se.demo.domain.service.ChartTypeInferenceService;
import vdt.se.demo.domain.valueObjects.TemplateType;

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
        QueryUseCaseService service = service(history);
        SearchRequest request = SearchRequest.builder()
                .question("show failed login")
                .build();

        QueryResult result = service.search(request);

        assertThat(result.getGeneratedDSL().has("query")).isTrue();
        assertThat(result.getSummary()).isEmpty();
        assertThat(history.rows).hasSize(1);
        assertThat(history.rows.getFirst().generatedDsl()).contains("\"match_all\"");
    }

    @Test
    void exportsHistorySnapshotAsCsv() {
        MemoryHistoryPort history = new MemoryHistoryPort();
        UUID id = UUID.randomUUID();
        history.rows.add(new QueryHistory(id, "soc-analyst-demo", "q", "{}", "s",
                null,
                vdt.se.demo.domain.valueObjects.ChartType.TABLE, 1,
                "[{\"user\":\"alice\",\"ip\":\"10.0.0.1\"}]", java.time.LocalDateTime.now()));
        QueryUseCaseService service = service(history);

        String csv = service.exportCsv(id);

        assertThat(csv).contains("user,ip");
        assertThat(csv).contains("\"alice\",\"10.0.0.1\"");
    }

    private QueryUseCaseService service(MemoryHistoryPort history) {
        ObjectMapper objectMapper = new ObjectMapper();
        SearchExecutionService executionService = new SearchExecutionService(
                new StubDslBuilder(objectMapper),
                new StubExecutor(),
                new NoopRefiner(),
                new ChartTypeInferenceService(objectMapper)
        );
        MemoryIntentCache intentCache = new MemoryIntentCache();
        AppProperties properties = new AppProperties();
        NoopDslCache dslCache = new NoopDslCache();
        AuditLogPort audit = auditLog -> {
        };
        QueryAuditService auditService = new QueryAuditService(audit, properties);
        QueryResultPersistenceService persistenceService = new QueryResultPersistenceService(history, objectMapper, properties);
        QuerySummaryService summaryService = new QuerySummaryService(new StubSummary(), Runnable::run);
        QueryDiagnosticService diagnosticService = new QueryDiagnosticService(
                countDsl -> 0,
                new DiagnosticDslVariantFactory(objectMapper),
                new ZeroResultDiagnosticClassifier(),
                Runnable::run
        );
        FinalizedSearchService finalizedSearchService = new FinalizedSearchService(
                executionService,
                persistenceService,
                auditService,
                dslCache,
                intentCache,
                objectMapper,
                summaryService,
                diagnosticService
        );
        QueryHashService queryHashService = new QueryHashService();
        SearchCacheContextService cacheContextService = new SearchCacheContextService(queryHashService, properties);
        CanonicalPlanBuilder canonicalPlanBuilder = new CanonicalPlanBuilder(
                new TemplateSelectionService(),
                new TemplateIntentSelector(),
                new GroupByResolver()
        );
        SearchPlanPreparationService planPreparationService = new SearchPlanPreparationService(
                new QueryRoutingService((left, right) -> 0.0d),
                new ContextRetrievalService(intentCache),
                new StubIntentExtraction(),
                question -> List.of(),
                new IntentMergeService(),
                new SearchIntentNormalizer(),
                canonicalPlanBuilder,
                cacheContextService
        );
        QuerySearchWorkflow searchWorkflow = new QuerySearchWorkflow(
                cacheContextService,
                planPreparationService,
                new SearchDslCacheLookupService(dslCache, executionService, persistenceService, auditService, objectMapper),
                new PendingConfirmationResultFactory(intentCache, objectMapper),
                finalizedSearchService,
                auditService
        );
        QueryConfirmationWorkflow confirmationWorkflow = new QueryConfirmationWorkflow(
                intentCache,
                new SearchIntentNormalizer(),
                canonicalPlanBuilder,
                finalizedSearchService,
                queryHashService,
                auditService
        );
        return new QueryUseCaseService(
                searchWorkflow,
                confirmationWorkflow,
                new QueryCsvExportService(history, objectMapper),
                history,
                summaryService,
                properties
        );
    }

    private static class StubIntentExtraction implements IntentExtractionPort {
        @Override
        public ExtractedIntent extract(IntentExtractionRequest request) {
            SearchIntent intent = SearchIntent.builder()
                    .intent(TemplateType.SIMPLE_SEARCH)
                    .textQuery("*")
                    .metric("COUNT")
                    .build();
            return ExtractedIntent.builder()
                    .intent(intent)
                    .provider("TEST")
                    .rawContent("{}")
                    .build();
        }
    }

    private static class StubSummary implements SummaryPort {
        @Override
        public String summarize(SearchRequest request, JsonNode generatedDsl, ExecutionResult executionResult) {
            return "summary text";
        }
    }

    private static class StubDslBuilder implements SearchDslBuilderPort {
        private final ObjectMapper objectMapper;

        private StubDslBuilder(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public JsonNode build(SearchRequest request, CanonicalQueryPlan plan) {
            return objectMapper.readTree("{\"query\":{\"match_all\":{}},\"size\":50}");
        }
    }

    private static class StubExecutor implements QueryExecutorPort {
        @Override
        public ExecutionResult execute(JsonNode generatedDsl) {
            return ExecutionResult.builder()
                    .results(List.of(Map.of("user", "alice")))
                    .aggregations(List.of())
                    .totalCount(1)
                    .build();
        }
    }

    private static class NoopRefiner implements QueryExecutionRefiner {
        @Override
        public QueryExecution refine(SearchRequest request, JsonNode generatedDsl, ExecutionResult executionResult) {
            return new QueryExecution(generatedDsl, executionResult);
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
        public List<QueryHistory> findRecent(String userIdentity, String sessionId, int limit) {
            return rows;
        }

        @Override
        public Optional<QueryHistory> findById(UUID id) {
            return rows.stream().filter(row -> row.id().equals(id)).findFirst();
        }
    }

    private static class NoopDslCache implements DslCachePort {
        @Override
        public Optional<String> findFinalizedDsl(String schemaVersion, String sessionId, String queryHash) {
            return Optional.empty();
        }

        @Override
        public void saveFinalizedDsl(String schemaVersion, String sessionId, String queryHash, String dsl) {
        }
    }

    private static class MemoryIntentCache implements IntentCachePort {
        @Override
        public Optional<CachedIntent> findLastClassifiedIntent(String sessionId) {
            return Optional.empty();
        }

        @Override
        public void saveLastClassifiedIntent(String sessionId, String schemaVersion, SearchIntent intent) {
        }

        @Override
        public void savePendingConfirmation(PendingConfirmation confirmation) {
        }

        @Override
        public Optional<PendingConfirmation> findPendingConfirmation(String confirmationId) {
            return Optional.empty();
        }
    }
}
