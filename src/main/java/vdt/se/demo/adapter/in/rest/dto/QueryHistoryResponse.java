package vdt.se.demo.adapter.in.rest.dto;

import vdt.se.demo.domain.model.QueryHistory;
import vdt.se.demo.domain.valueObjects.ChartType;

import java.time.LocalDateTime;
import java.util.UUID;

public record QueryHistoryResponse(
        UUID id,
        String userIdentity,
        String nlQuery,
        String generatedDsl,
        String summary,
        ChartType chartType,
        int totalCount,
        LocalDateTime createdAt
) {
    public static QueryHistoryResponse from(QueryHistory history) {
        return new QueryHistoryResponse(
                history.id(),
                history.userIdentity(),
                history.nlQuery(),
                history.generatedDsl(),
                history.summary(),
                history.chartType(),
                history.totalCount(),
                history.createdAt()
        );
    }
}
