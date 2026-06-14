package vdt.se.demo.application.port.outboundPort;

import vdt.se.demo.domain.model.AuditLog;

public interface AuditLogPort {
    void saveAsync(AuditLog auditLog);
}
