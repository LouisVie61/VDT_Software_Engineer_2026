package vdt.se.demo.application.service.query;

import vdt.se.demo.application.dto.SearchRequest;
import vdt.se.demo.application.port.inboundPort.QueryUseCase;
import vdt.se.demo.application.port.outboundPort.audit.AuditLogPort;
import vdt.se.demo.application.port.outboundPort.history.QueryHistoryPort;
import vdt.se.demo.domain.model.AuditLog;
import vdt.se.demo.domain.model.QueryHistory;
import vdt.se.demo.domain.model.QueryResult;
import vdt.se.demo.domain.model.SummaryResult;
import vdt.se.demo.domain.model.ExecutionResult;
import vdt.se.demo.domain.valueObjects.AuditStatus;
import vdt.se.demo.domain.valueObjects.SummaryStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class QueryUseCaseService implements QueryUseCase {
    private static final Logger log = LoggerFactory.getLogger(QueryUseCaseService.class);

    private final IqlSearchWorkflow searchWorkflow;
    private final QueryCsvExportService csvExportService;
    private final QueryHistoryPort queryHistoryPort;
    private final AuditLogPort auditLogPort;
    private final QuerySummaryService summaryService;
    private final String defaultUserId;

    public QueryUseCaseService(IqlSearchWorkflow searchWorkflow,
                               QueryCsvExportService csvExportService,
                               QueryHistoryPort queryHistoryPort,
                               AuditLogPort auditLogPort,
                               QuerySummaryService summaryService,
                               String defaultUserId) {
        this.searchWorkflow = searchWorkflow;
        this.csvExportService = csvExportService;
        this.queryHistoryPort = queryHistoryPort;
        this.auditLogPort = auditLogPort;
        this.summaryService = summaryService;
        this.defaultUserId = defaultUserId;
    }

    @Override
    public QueryResult search(SearchRequest request) {
        long started = System.nanoTime();
        QueryResult result = searchWorkflow.search(request);
        ExecutionResult execution = new ExecutionResult(rows(result.getResults()), rows(result.getAggregations()),
                result.getTotalCount(), result.getWarnings());
        summaryService.schedule(result.getId(), request, result.getGeneratedDSL(), execution, result.getChartType());
        result.setSummaryStatus(SummaryStatus.PENDING);
        auditSearch(request, result, elapsedMillis(started));
        return result;
    }

    private void auditSearch(SearchRequest request, QueryResult result, long executionTimeMs) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setId(result.getId());
            auditLog.setUserIdentity(defaultUserId);
            auditLog.setTimestamp(LocalDateTime.now());
            auditLog.setNlQuery(request.getQuestion());
            auditLog.setGeneratedDSL(result.getGeneratedDSL() == null ? null : result.getGeneratedDSL().toString());
            auditLog.setResultsCount(result.getTotalCount());
            auditLog.setExecutionTimeMs(executionTimeMs);
            auditLog.setStatus(AuditStatus.SUCCESS);
            auditLog.setSessionId(request.getSessionId());
            auditLog.setSelectedTemplate(result.getSelectedTemplate());
            auditLog.setCacheHit(result.isCacheHit());
            auditLogPort.saveAsync(auditLog);
        } catch (Exception ex) {
            log.warn("Audit log scheduling failed: queryId={}, error={}", result.getId(), ex.getMessage());
        }
    }

    private long elapsedMillis(long startedNanos) {
        return Math.max(0, (System.nanoTime() - startedNanos) / 1_000_000);
    }

    @SuppressWarnings("unchecked")
    private List<java.util.Map<String, Object>> rows(Object value) {
        return value instanceof List<?> list
                ? (List<java.util.Map<String, Object>>) (List<?>) list
                : List.of();
    }

    @Override
    public List<QueryHistory> history(String userIdentity, int limit) {
        return history(userIdentity, null, limit);
    }

    @Override
    public List<QueryHistory> history(String userIdentity, String sessionId, int limit) {
        String user = userIdentity == null || userIdentity.isBlank() ? defaultUserId : userIdentity;
        return sessionId == null || sessionId.isBlank()
                ? queryHistoryPort.findRecent(user, limit)
                : queryHistoryPort.findRecent(user, sessionId, limit);
    }

    @Override
    public String exportCsv(UUID queryId) {
        return csvExportService.exportCsv(queryId);
    }

    @Override
    public SummaryResult summary(UUID queryId) {
        return summaryService.find(queryId);
    }
}
