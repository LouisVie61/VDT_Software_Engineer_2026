package vdt.se.demo.application.port.outboundPort.audit;

import vdt.se.demo.domain.model.AuditLog;

public interface AuditLogPort {
    void saveAsync(AuditLog auditLog);
}

