package vdt.se.demo.adapter.out.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;
import vdt.se.demo.application.port.outboundPort.audit.AuditLogPort;
import vdt.se.demo.domain.model.AuditLog;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public class AuditLogAdapter implements AuditLogPort {
    private static final Logger log = LoggerFactory.getLogger(AuditLogAdapter.class);

    private final JdbcTemplate jdbcTemplate;

    public AuditLogAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Async("auditTaskExecutor")
    @Override
    public void saveAsync(AuditLog auditLog) {
        log.debug("Audit log write started: auditId={}, status={}, provider={}",
                auditLog.getId(), auditLog.getStatus(), auditLog.getLlmProvider());
        jdbcTemplate.update("""
                        INSERT INTO audit_logs (
                            id, user_identity, event_timestamp, nl_query, generated_dsl,
                            results_count, execution_time_ms, status, llm_provider, error_message,
                            session_id, predicted_intent, override_intent, selected_template,
                            confidence_scores, cache_hit, diagnostic_classification
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                value(auditLog.getId(), UUID.randomUUID()),
                auditLog.getUserIdentity(),
                value(auditLog.getTimestamp(), LocalDateTime.now()),
                auditLog.getNlQuery(),
                auditLog.getGeneratedDSL(),
                auditLog.getResultsCount(),
                auditLog.getExecutionTimeMs(),
                auditLog.getStatus() == null ? "FAILED" : auditLog.getStatus().name(),
                auditLog.getLlmProvider(),
                auditLog.getErrorMessage(),
                auditLog.getSessionId(),
                auditLog.getPredictedIntent(),
                auditLog.getOverrideIntent(),
                auditLog.getSelectedTemplate(),
                auditLog.getConfidenceScores(),
                Boolean.TRUE.equals(auditLog.getCacheHit()),
                auditLog.getDiagnosticClassification()
        );
        log.debug("Audit log write completed: auditId={}", auditLog.getId());
    }

    private <T> T value(T value, T defaultValue) {
        return value == null ? defaultValue : value;
    }
}
