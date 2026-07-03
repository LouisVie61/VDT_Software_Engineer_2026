package vdt.se.demo.domain.iql;

import java.time.Instant;

public record SessionState(String sessionId, IqlQuery lastQuery,
                           ResultSummary lastResultSummary, Instant updatedAt) {}
