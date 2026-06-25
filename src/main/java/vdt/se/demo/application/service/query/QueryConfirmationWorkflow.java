package vdt.se.demo.application.service.query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vdt.se.demo.application.dto.ConfirmSearchRequest;
import vdt.se.demo.application.dto.SearchRequest;
import vdt.se.demo.application.port.outboundPort.cache.IntentCachePort;
import vdt.se.demo.application.service.cache.QueryHashService;
import vdt.se.demo.application.service.intent.SearchIntentNormalizer;
import vdt.se.demo.application.service.template.CanonicalPlanBuilder;
import vdt.se.demo.domain.exception.BadQueryException;
import vdt.se.demo.domain.model.CanonicalQueryPlan;
import vdt.se.demo.domain.model.PendingConfirmation;
import vdt.se.demo.domain.model.QueryResult;
import vdt.se.demo.domain.model.RoutingHint;
import vdt.se.demo.domain.model.SearchIntent;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class QueryConfirmationWorkflow {
    private static final Logger log = LoggerFactory.getLogger(QueryConfirmationWorkflow.class);

    private final IntentCachePort intentCachePort;
    private final SearchIntentNormalizer searchIntentNormalizer;
    private final CanonicalPlanBuilder canonicalPlanBuilder;
    private final FinalizedSearchService finalizedSearchService;
    private final QueryHashService queryHashService;
    private final QueryAuditService auditService;

    public QueryConfirmationWorkflow(IntentCachePort intentCachePort, SearchIntentNormalizer searchIntentNormalizer,
                                     CanonicalPlanBuilder canonicalPlanBuilder,
                                     FinalizedSearchService finalizedSearchService, QueryHashService queryHashService,
                                     QueryAuditService auditService) {
        this.intentCachePort = intentCachePort;
        this.searchIntentNormalizer = searchIntentNormalizer;
        this.canonicalPlanBuilder = canonicalPlanBuilder;
        this.finalizedSearchService = finalizedSearchService;
        this.queryHashService = queryHashService;
        this.auditService = auditService;
    }

    public QueryResult confirm(ConfirmSearchRequest request) {
        long startedAt = System.nanoTime();
        log.debug("Confirmation workflow started: confirmationId={}, sessionId={}, editedIntent={}",
                request.getConfirmationId(), request.getSessionId(), request.getEditedIntent() != null);
        PendingConfirmation pending = intentCachePort.findPendingConfirmation(request.getConfirmationId())
                .orElseThrow(() -> new BadQueryException("Confirmation request not found or expired"));
        log.info("Pending confirmation cache hit: confirmationId={}, pendingSessionId={}, schemaVersion={}, template={}",
                request.getConfirmationId(), pending.sessionId(), pending.schemaVersion(), pending.templateSelection().type());
        if (request.getSessionId() != null && !request.getSessionId().isBlank()
                && !request.getSessionId().equals(pending.sessionId())) {
            throw new BadQueryException("Confirmation belongs to a different session");
        }
        SearchRequest searchRequest = searchRequest(request, pending);
        SearchIntent intent = searchIntentNormalizer.normalize(searchRequest,
                request.getEditedIntent() == null ? pending.intent() : request.getEditedIntent());
        RoutingHint routingHint = RoutingHint.builder()
                .templateType(pending.templateSelection().type())
                .confidence(1.0d)
                .reason("confirmed template")
                .build();
        CanonicalQueryPlan plan = canonicalPlanBuilder.build(
                normalizedQuery(searchRequest),
                pending.schemaVersion(),
                pending.sessionId(),
                routingHint,
                pending.intent(),
                intent,
                "CONFIRMED",
                null);
        if (plan.templateSelection() == null) {
            String message = plan.warnings().isEmpty() ? "Confirmation validation failed" : plan.warnings().getFirst().message();
            log.debug("Confirmation validation rejected: confirmationId={}, error={}",
                    request.getConfirmationId(), message);
            throw new BadQueryException(message);
        }
        if (plan.warnings().stream().anyMatch(warning -> "GROUP_BY_REQUIRED".equals(warning.code()))) {
            throw new BadQueryException("Aggregation requires a grouping field before execution.");
        }
        if (plan.warnings().stream().anyMatch(warning -> "TOP_N_REQUIRED".equals(warning.code()))) {
            throw new BadQueryException("Aggregation requires Top N before execution.");
        }
        log.debug("Confirmation validation completed: confirmationId={}, template={}, groupBy={}, size={}",
                request.getConfirmationId(), plan.templateSelection().type(), plan.templateSelection().groupBy(),
                plan.templateSelection().size());
        UUID queryId = UUID.randomUUID();
        LocalDateTime started = LocalDateTime.now();
        try {
            QueryResult result = finalizedSearchService.execute(queryId, searchRequest, plan,
                    new FinalizedSearchService.CacheContext(
                            pending.schemaVersion(),
                            pending.sessionId(),
                            queryHashService.hash(searchRequest, plan)),
                    started);
            log.debug("Confirmation workflow completed: confirmationId={}, queryId={}, totalCount={}, elapsedMs={}",
                    request.getConfirmationId(), queryId, result.getTotalCount(),
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt));
            return result;
        } catch (RuntimeException e) {
            log.debug("Confirmation workflow failed: confirmationId={}, queryId={}, elapsedMs={}, error={}",
                    request.getConfirmationId(), queryId,
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt), e.getMessage());
            auditService.failure(queryId, searchRequest, null, started, "CONFIRMED", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.debug("Confirmation workflow failed: confirmationId={}, queryId={}, elapsedMs={}, error={}",
                    request.getConfirmationId(), queryId,
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt), e.getMessage());
            auditService.failure(queryId, searchRequest, null, started, "CONFIRMED", e.getMessage());
            throw new BadQueryException("Confirmed search failed", e);
        }
    }

    private SearchRequest searchRequest(ConfirmSearchRequest request, PendingConfirmation pending) {
        return SearchRequest.builder()
                .question(pending.question())
                .sessionId(pending.sessionId())
                .page(request.getPage())
                .pageSize(request.getPageSize())
                .from(pending.from())
                .to(pending.to())
                .severity(pending.severity())
                .eventType(pending.eventType())
                .user(pending.user())
                .host(pending.host())
                .ip(pending.ip())
                .build();
    }

    private String normalizedQuery(SearchRequest request) {
        return request.getQuestion() == null ? "" : request.getQuestion().strip().toLowerCase(java.util.Locale.ROOT)
                .replaceAll("\\s+", " ");
    }
}
