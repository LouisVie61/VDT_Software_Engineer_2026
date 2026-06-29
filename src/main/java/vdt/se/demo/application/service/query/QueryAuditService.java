package vdt.se.demo.application.service.query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vdt.se.demo.adapter.config.AppProperties;
import vdt.se.demo.application.dto.SearchRequest;
import vdt.se.demo.application.port.outboundPort.audit.AuditLogPort;
import vdt.se.demo.domain.model.AuditLog;
import vdt.se.demo.domain.model.CanonicalQueryPlan;
import vdt.se.demo.domain.model.ZeroResultDiagnostic;
import vdt.se.demo.domain.valueObjects.AuditStatus;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

public class QueryAuditService {
    private static final Logger log = LoggerFactory.getLogger(QueryAuditService.class);

    private final AuditLogPort auditLogPort;
    private final AppProperties properties;

    public QueryAuditService(AuditLogPort auditLogPort, AppProperties properties) {
        this.auditLogPort = auditLogPort;
        this.properties = properties;
    }

    public void success(UUID id, SearchRequest request, String dsl, Integer count, LocalDateTime started,
                        CanonicalQueryPlan plan, boolean cacheHit, ZeroResultDiagnostic diagnostic) {
        log.debug("Submitting audit success: queryId={}, provider={}, count={}", id, plan.provider(), count);
        auditLogPort.saveAsync(audit(id, request, dsl, count, started, AuditStatus.SUCCESS, plan, null, cacheHit,
                diagnostic));
    }

    public void failure(UUID id, SearchRequest request, String dsl, LocalDateTime started, String provider,
                        String errorMessage) {
        log.debug("Submitting audit failure: queryId={}, provider={}, error={}", id, provider, errorMessage);
        CanonicalQueryPlan plan = CanonicalQueryPlan.builder()
                .provider(provider)
                .sessionId(request == null ? null : request.getSessionId())
                .build();
        auditLogPort.saveAsync(audit(id, request, dsl, null, started, AuditStatus.FAILED, plan, errorMessage, false,
                null));
    }

    private AuditLog audit(UUID id, SearchRequest request, String dsl, Integer count, LocalDateTime started,
                           AuditStatus status, CanonicalQueryPlan plan, String errorMessage, boolean cacheHit,
                           ZeroResultDiagnostic diagnostic) {
        AuditLog auditLog = new AuditLog();
        auditLog.setId(id);
        auditLog.setUserIdentity(properties.getUser().getDefaultId());
        auditLog.setTimestamp(LocalDateTime.now());
        auditLog.setNlQuery(request == null ? null : request.getQuestion());
        auditLog.setGeneratedDSL(dsl);
        auditLog.setResultsCount(count);
        auditLog.setExecutionTimeMs(Duration.between(started, LocalDateTime.now()).toMillis());
        auditLog.setStatus(status);
        auditLog.setLlmProvider(plan == null ? null : plan.provider());
        auditLog.setErrorMessage(errorMessage);
        auditLog.setSessionId(plan == null ? null : plan.sessionId());
        auditLog.setPredictedIntent(plan == null || plan.routingHint() == null || plan.routingHint().templateType() == null
                ? null
                : plan.routingHint().templateType().name());
        auditLog.setOverrideIntent(plan == null ? null : plan.overrideIntent());
        auditLog.setSelectedTemplate(plan == null || plan.templateSelection() == null ? null : plan.templateSelection().type().name());
        auditLog.setConfidenceScores(plan == null ? "{}" : plan.confidenceScores().toString());
        auditLog.setCacheHit(cacheHit);
        auditLog.setDiagnosticClassification(diagnostic == null ? null : diagnostic.reasonCode());
        return auditLog;
    }
}
