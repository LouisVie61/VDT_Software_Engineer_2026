package vdt.se.demo.domain.model;

public record IngestResult(
        long totalRows,
        long indexedRows,
        long failedRows,
        String indexName,
        long durationMs
) {
}
