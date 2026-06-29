package vdt.se.demo.application.service.query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.application.port.outboundPort.history.QueryHistoryPort;
import vdt.se.demo.application.port.outboundPort.execution.QueryExecutorPort;
import vdt.se.demo.domain.exception.BadQueryException;
import vdt.se.demo.domain.model.ExecutionResult;
import vdt.se.demo.domain.model.QueryHistory;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class QueryCsvExportService {
    private static final Logger log = LoggerFactory.getLogger(QueryCsvExportService.class);

    private final QueryHistoryPort queryHistoryPort;
    private final ObjectMapper objectMapper;
    private final QueryExecutorPort queryExecutorPort;

    public QueryCsvExportService(QueryHistoryPort queryHistoryPort, ObjectMapper objectMapper,
                                 QueryExecutorPort queryExecutorPort) {
        this.queryHistoryPort = queryHistoryPort;
        this.objectMapper = objectMapper;
        this.queryExecutorPort = queryExecutorPort;
    }

    public String exportCsv(UUID queryId) {
        log.debug("CSV export started: queryId={}", queryId);
        QueryHistory history = queryHistoryPort.findById(queryId)
                .orElseThrow(() -> new BadQueryException("Query history not found: " + queryId));
        try {
            JsonNode dsl = objectMapper.readTree(history.generatedDsl());
            ExecutionResult executionResult = queryExecutorPort.execute(dsl);
            JsonNode rows = objectMapper.readTree(objectMapper.writeValueAsString(executionResult.results()));
            if (!rows.isArray() || rows.isEmpty()) {
                log.debug("CSV export completed with no rows after DSL re-execution: queryId={}", queryId);
                return "";
            }
            Set<String> headers = headers(rows);
            StringBuilder csv = new StringBuilder(String.join(",", headers)).append("\n");
            for (JsonNode row : rows) {
                appendRow(csv, headers, row);
            }
            log.debug("CSV export completed: queryId={}, rows={}, columns={}", queryId, rows.size(), headers.size());
            return csv.toString();
        } catch (Exception e) {
            log.debug("CSV export failed: queryId={}, error={}", queryId, e.getMessage());
            throw new BadQueryException("Cannot export query result as CSV", e);
        }
    }

    private Set<String> headers(JsonNode rows) {
        Set<String> headers = new LinkedHashSet<>();
        for (JsonNode row : rows) {
            for (Map.Entry<String, JsonNode> field : row.properties()) {
                headers.add(field.getKey());
            }
        }
        return headers;
    }

    private void appendRow(StringBuilder csv, Set<String> headers, JsonNode row) {
        boolean first = true;
        for (String header : headers) {
            if (!first) {
                csv.append(",");
            }
            first = false;
            JsonNode value = row.get(header);
            csv.append(escapeCsv(value == null || value.isNull() ? "" : value.asString()));
        }
        csv.append("\n");
    }

    private String escapeCsv(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
