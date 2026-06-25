package vdt.se.demo.domain.model;

import vdt.se.demo.domain.valueObjects.ChartType;

import java.time.LocalDateTime;
import java.util.UUID;

public record QueryHistory(
        UUID id,
        String userIdentity,
        String sessionId,
        String nlQuery,
        String generatedDsl,
        String summary,
        ChartType chartType,
        int totalCount,
        String resultSnapshot,
        LocalDateTime createdAt
) {
}
