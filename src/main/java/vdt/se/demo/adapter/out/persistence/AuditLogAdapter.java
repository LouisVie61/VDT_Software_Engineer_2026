package vdt.se.demo.adapter.out.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;
import vdt.se.demo.application.port.outboundPort.AuditLogPort;
import vdt.se.demo.domain.model.AuditLog;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public class AuditLogAdapter implements AuditLogPort {

    private final JdbcTemplate jdbcTemplate;

    public AuditLogAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Async
    @Override
    public void saveAsync(AuditLog auditLog) {
        jdbcTemplate.update("""
                        INSERT INTO audit_logs (
                            id, user_identity, event_timestamp, nl_query, generated_dsl,
                            results_count, execution_time_ms, status, llm_provider, error_message
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
                auditLog.getErrorMessage()
        );
    }

    private <T> T value(T value, T defaultValue) {
        return value == null ? defaultValue : value;
    }
}
