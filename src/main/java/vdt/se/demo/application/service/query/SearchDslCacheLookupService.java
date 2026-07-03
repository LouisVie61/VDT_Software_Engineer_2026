package vdt.se.demo.application.service.query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.application.dto.SearchRequest;
import vdt.se.demo.application.port.outboundPort.cache.DslCachePort;
import vdt.se.demo.application.service.execution.SearchExecutionService;
import vdt.se.demo.domain.model.CanonicalQueryPlan;
import vdt.se.demo.domain.model.QueryResult;
import vdt.se.demo.domain.valueObjects.SummaryStatus;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public class SearchDslCacheLookupService {
    private static final Logger log = LoggerFactory.getLogger(SearchDslCacheLookupService.class);

    private final DslCachePort dslCachePort;
    private final SearchExecutionService searchExecutionService;
    private final QueryResultPersistenceService persistenceService;
    private final QueryAuditService auditService;
    private final ObjectMapper objectMapper;
    private final QuerySummaryService summaryService;
    private final QueryDiagnosticService diagnosticService;

    public SearchDslCacheLookupService(DslCachePort dslCachePort, SearchExecutionService searchExecutionService,
                                       QueryResultPersistenceService persistenceService, QueryAuditService auditService,
                                       ObjectMapper objectMapper, QuerySummaryService summaryService,
                                       QueryDiagnosticService diagnosticService) {
        this.dslCachePort = dslCachePort;
        this.searchExecutionService = searchExecutionService;
        this.persistenceService = persistenceService;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
        this.summaryService = summaryService;
        this.diagnosticService = diagnosticService;
    }

    public Optional<QueryResult> executeCached(UUID queryId, SearchRequest request,
                                               FinalizedSearchService.CacheContext cache, LocalDateTime started,
                                               CanonicalQueryPlan plan) throws Exception {
        Optional<String> cachedDsl = dslCachePort.findFinalizedDsl(cache.schemaVersion(), cache.sessionId(), cache.queryHash());
        if (cachedDsl.isEmpty()) {
            log.debug("Search DSL cache miss: queryId={}, sessionId={}, schemaVersion={}, queryHash={}",
                    queryId, cache.sessionId(), cache.schemaVersion(), cache.queryHash());
            return Optional.empty();
        }

        log.info("Search DSL cache hit: queryId={}, sessionId={}, schemaVersion={}, queryHash={}",
                queryId, cache.sessionId(), cache.schemaVersion(), cache.queryHash());
        JsonNode dsl = objectMapper.readTree(cachedDsl.get());
        CanonicalQueryPlan cachedPlan = cachedPlan(plan);
        SearchExecutionService.ExecutedSearch executed = searchExecutionService.executeDsl(queryId, request, dsl, cachedPlan);
        QueryResult result = executed.result();
        result.setCacheHit(true);
        if (result.getTotalCount() == 0) {
            result.setDiagnostic(diagnosticService.diagnose(request, cachedPlan, executed.dsl()));
        }
        result.setSummaryStatus(hasSummaryPayload(executed) ? SummaryStatus.PENDING : SummaryStatus.NOT_REQUIRED);
        summaryService.schedule(queryId, request, executed.dsl(), executed.executionResult(), result.getChartType());
        persistenceService.save(queryId, request, result, cachedDsl.get());
        auditService.success(queryId, request, cachedDsl.get(), result.getTotalCount(), started, cachedPlan, true,
                result.getDiagnostic());
        return Optional.of(result);
    }

    private boolean hasSummaryPayload(SearchExecutionService.ExecutedSearch executed) {
        return !executed.executionResult().results().isEmpty() || !executed.executionResult().aggregations().isEmpty();
    }

    private CanonicalQueryPlan cachedPlan(CanonicalQueryPlan plan) {
        return CanonicalQueryPlan.builder()
                .normalizedQuery(plan.normalizedQuery())
                .schemaVersion(plan.schemaVersion())
                .sessionId(plan.sessionId())
                .routingHint(plan.routingHint())
                .extractedFields(plan.extractedFields())
                .mergedIntent(plan.mergedIntent())
                .templateSelection(plan.templateSelection())
                .chartHint(plan.chartHint())
                .confidenceScores(plan.confidenceScores())
                .overrideIntent(plan.overrideIntent())
                .overrideReason(plan.overrideReason())
                .semanticSpans(plan.semanticSpans())
                .warnings(plan.warnings())
                .provider("CACHE")
                .build();
    }
}
