package vdt.se.demo.adapter.in.rest.dto;

import vdt.se.demo.domain.model.IngestResult;

public record IngestResponse(
        long totalRows,
        long indexedRows,
        long failedRows,
        String indexName,
        long durationMs
) {
    public static IngestResponse from(IngestResult result) {
        return new IngestResponse(
                result.totalRows(),
                result.indexedRows(),
                result.failedRows(),
                result.indexName(),
                result.durationMs()
        );
    }
}
