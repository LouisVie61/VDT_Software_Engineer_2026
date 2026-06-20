package vdt.se.demo.application.service.query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vdt.se.demo.application.dto.SearchRequest;
import vdt.se.demo.domain.exception.BadQueryException;
import vdt.se.demo.domain.model.CanonicalQueryPlan;
import vdt.se.demo.domain.model.QueryResult;
import vdt.se.demo.domain.model.RoutingHint;
import vdt.se.demo.domain.valueObjects.TemplateType;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class QuerySearchWorkflow {
    private static final Logger log = LoggerFactory.getLogger(QuerySearchWorkflow.class);

    private final SearchCacheContextService cacheContextService;
    private final SearchPlanPreparationService planPreparationService;
    private final SearchDslCacheLookupService cacheLookupService;
    private final PendingConfirmationResultFactory confirmationResultFactory;
    private final FinalizedSearchService finalizedSearchService;
    private final QueryAuditService auditService;

    public QuerySearchWorkflow(SearchCacheContextService cacheContextService,
                               SearchPlanPreparationService planPreparationService,
                               SearchDslCacheLookupService cacheLookupService,
                               PendingConfirmationResultFactory confirmationResultFactory,
                               FinalizedSearchService finalizedSearchService,
                               QueryAuditService auditService) {
        this.cacheContextService = cacheContextService;
        this.planPreparationService = planPreparationService;
        this.cacheLookupService = cacheLookupService;
        this.confirmationResultFactory = confirmationResultFactory;
        this.finalizedSearchService = finalizedSearchService;
        this.auditService = auditService;
    }

    public QueryResult search(SearchRequest request) {
        UUID queryId = UUID.randomUUID();
        LocalDateTime started = LocalDateTime.now();
        long startedAt = System.nanoTime();
        String provider = null;
        try {
            FinalizedSearchService.CacheContext requestCache = cacheContextService.forRequest(request);
            log.debug("Search workflow started: queryId={}, sessionId={}, schemaVersion={}, queryHash={}, page={}, pageSize={}",
                    queryId, requestCache.sessionId(), requestCache.schemaVersion(), requestCache.queryHash(),
                    request.getPage(), request.getPageSize());

            SearchPlanPreparationService.PreparedSearchPlan prepared =
                    planPreparationService.prepare(request, requestCache);
            provider = prepared.provider();
            CanonicalQueryPlan plan = prepared.plan();
            validatePlan(queryId, plan);

            if (requiresConfirmation(plan, prepared.routingHint())) {
                log.debug("Search workflow requires confirmation: queryId={}, template={}, elapsedMs={}",
                        queryId, plan.templateSelection().type(), elapsedMs(startedAt));
                return confirmationResultFactory.create(queryId, request, requestCache, plan);
            }

            FinalizedSearchService.CacheContext planCache = cacheContextService.forPlan(request, plan);
            Optional<QueryResult> cached = cacheLookupService.executeCached(queryId, request, planCache, started, plan);
            if (cached.isPresent()) {
                log.debug("Search workflow completed from cache: queryId={}, totalCount={}, elapsedMs={}",
                        queryId, cached.get().getTotalCount(), elapsedMs(startedAt));
                return cached.get();
            }

            QueryResult result = finalizedSearchService.execute(queryId, request, plan, planCache, started);
            log.debug("Search workflow completed: queryId={}, totalCount={}, template={}, elapsedMs={}",
                    queryId, result.getTotalCount(), plan.templateSelection().type(), elapsedMs(startedAt));
            return result;
        } catch (RuntimeException e) {
            log.debug("Search workflow failed: queryId={}, provider={}, elapsedMs={}, error={}",
                    queryId, provider, elapsedMs(startedAt), e.getMessage());
            auditService.failure(queryId, request, null, started, provider, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.debug("Search workflow failed: queryId={}, provider={}, elapsedMs={}, error={}",
                    queryId, provider, elapsedMs(startedAt), e.getMessage());
            auditService.failure(queryId, request, null, started, provider, e.getMessage());
            throw new BadQueryException("Search failed", e);
        }
    }

    private void validatePlan(UUID queryId, CanonicalQueryPlan plan) {
        if (plan.templateSelection() == null) {
            String message = plan.warnings().isEmpty() ? "Search plan validation failed" : plan.warnings().getFirst().message();
            log.debug("Search plan validation rejected: queryId={}, error={}", queryId, message);
            throw new BadQueryException(message);
        }
        log.debug("Search plan validation completed: queryId={}, template={}, groupBy={}, size={}, warnings={}",
                queryId, plan.templateSelection().type(), plan.templateSelection().groupBy(),
                plan.templateSelection().size(), plan.warnings().size());
    }

    private boolean requiresConfirmation(CanonicalQueryPlan plan, RoutingHint routingHint) {
        if (plan.overrideIntent() != null) {
            return true;
        }
        if (plan.warnings().stream().anyMatch(warning -> "GROUP_BY_REQUIRED".equals(warning.code()))) {
            return true;
        }
        boolean lowConfidence = plan.confidenceScores().values().stream().anyMatch(score -> score < 0.60d);
        return lowConfidence || plan.templateSelection().type() != TemplateType.SIMPLE_SEARCH
                && (routingHint == null || routingHint.confidence() < 0.70d);
    }

    private long elapsedMs(long startedAt) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
    }
}
