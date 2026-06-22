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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class SearchDslCacheLookupService {
    private static final Logger log = LoggerFactory.getLogger(SearchDslCacheLookupService.class);

    private final DslCachePort dslCachePort;
    private final SearchExecutionService searchExecutionService;
    private final QueryResultPersistenceService persistenceService;
    private final QueryAuditService auditService;
    private final ObjectMapper objectMapper;

    public SearchDslCacheLookupService(DslCachePort dslCachePort, SearchExecutionService searchExecutionService,
                                       QueryResultPersistenceService persistenceService, QueryAuditService auditService,
                                       ObjectMapper objectMapper) {
        this.dslCachePort = dslCachePort;
        this.searchExecutionService = searchExecutionService;
        this.persistenceService = persistenceService;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
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
        result.setSummaryStatus(result.getAggregations() instanceof List<?> list && !list.isEmpty()
                ? SummaryStatus.PENDING
                : SummaryStatus.NOT_REQUIRED);
        persistenceService.save(queryId, request, result, cachedDsl.get());
        auditService.success(queryId, request, cachedDsl.get(), result.getTotalCount(), started, cachedPlan, true);
        return Optional.of(result);
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
