package vdt.se.demo.application.service.query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.application.dto.SearchRequest;
import vdt.se.demo.application.port.outboundPort.cache.DslCachePort;
import vdt.se.demo.application.port.outboundPort.cache.IntentCachePort;
import vdt.se.demo.application.service.execution.SearchExecutionService;
import vdt.se.demo.domain.model.CanonicalQueryPlan;
import vdt.se.demo.domain.model.QueryResult;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class FinalizedSearchService {
    private static final Logger log = LoggerFactory.getLogger(FinalizedSearchService.class);

    private final SearchExecutionService searchExecutionService;
    private final QueryResultPersistenceService persistenceService;
    private final QueryAuditService auditService;
    private final DslCachePort dslCachePort;
    private final IntentCachePort intentCachePort;
    private final ObjectMapper objectMapper;
    private final QuerySummaryService summaryService;
    private final QueryDiagnosticService diagnosticService;

    public FinalizedSearchService(SearchExecutionService searchExecutionService,
                                  QueryResultPersistenceService persistenceService,
                                  QueryAuditService auditService,
                                  DslCachePort dslCachePort,
                                  IntentCachePort intentCachePort,
                                  ObjectMapper objectMapper,
                                  QuerySummaryService summaryService,
                                  QueryDiagnosticService diagnosticService) {
        this.searchExecutionService = searchExecutionService;
        this.persistenceService = persistenceService;
        this.auditService = auditService;
        this.dslCachePort = dslCachePort;
        this.intentCachePort = intentCachePort;
        this.objectMapper = objectMapper;
        this.summaryService = summaryService;
        this.diagnosticService = diagnosticService;
    }

    public QueryResult execute(UUID queryId, SearchRequest request, CanonicalQueryPlan plan,
                               CacheContext cache, LocalDateTime started) throws Exception {
        long startedAt = System.nanoTime();
        log.debug("Finalized search execution started: queryId={}, provider={}, template={}, sessionId={}, schemaVersion={}, queryHash={}",
                queryId, plan.provider(), plan.templateSelection().type(), cache.sessionId(), cache.schemaVersion(), cache.queryHash());
        SearchExecutionService.ExecutedSearch executed = searchExecutionService.execute(queryId, request, plan);
        String generatedDsl = objectMapper.writeValueAsString(executed.dsl());
        dslCachePort.saveFinalizedDsl(cache.schemaVersion(), cache.sessionId(), cache.queryHash(), generatedDsl);
        log.debug("Finalized DSL cached: queryId={}, sessionId={}, schemaVersion={}, queryHash={}",
                queryId, cache.sessionId(), cache.schemaVersion(), cache.queryHash());
        intentCachePort.saveLastClassifiedIntent(cache.sessionId(), cache.schemaVersion(), plan.mergedIntent());
        log.debug("Last classified intent saved: queryId={}, sessionId={}, schemaVersion={}, intent={}",
                queryId, cache.sessionId(), cache.schemaVersion(), plan.templateSelection().type());
        QueryResult result = executed.result();
        result.setCacheHit(false);
        if (result.getTotalCount() == 0) {
            result.setDiagnostic(diagnosticService.diagnose(request, plan, executed.dsl()));
        }
        summaryService.schedule(queryId, request, executed.dsl(), executed.executionResult(), result.getChartType());
        persistenceService.save(queryId, request, executed.result(), generatedDsl);
        log.debug("Search history persisted: queryId={}, totalCount={}", queryId, executed.result().getTotalCount());
        auditService.success(queryId, request, generatedDsl, executed.result().getTotalCount(), started, plan, false);
        log.debug("Audit success submitted: queryId={}, provider={}, elapsedMs={}",
                queryId, plan.provider(), TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt));
        return executed.result();
    }

    public record CacheContext(String schemaVersion, String sessionId, String queryHash) {
    }
}
