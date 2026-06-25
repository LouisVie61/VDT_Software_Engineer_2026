package vdt.se.demo.domain.model;

import lombok.Builder;

import java.time.Instant;

@Builder
public record PendingConfirmation(
        String confirmationId,
        String schemaVersion,
        String sessionId,
        String question,
        String from,
        String to,
        String severity,
        String eventType,
        String user,
        String host,
        String ip,
        SearchIntent intent,
        TemplateSelection templateSelection,
        Instant createdAt
) {
}
