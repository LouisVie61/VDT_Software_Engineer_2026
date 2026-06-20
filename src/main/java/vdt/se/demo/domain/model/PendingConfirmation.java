package vdt.se.demo.domain.model;

import lombok.Builder;

import java.time.Instant;

@Builder
public record PendingConfirmation(
        String confirmationId,
        String schemaVersion,
        String sessionId,
        String question,
        SearchIntent intent,
        TemplateSelection templateSelection,
        Instant createdAt
) {
}
