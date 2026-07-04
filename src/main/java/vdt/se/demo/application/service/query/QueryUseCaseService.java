package vdt.se.demo.application.service.query;

import vdt.se.demo.application.dto.SearchRequest;
import vdt.se.demo.application.port.inboundPort.QueryUseCase;
import vdt.se.demo.application.port.outboundPort.history.QueryHistoryPort;
import vdt.se.demo.domain.model.QueryHistory;
import vdt.se.demo.domain.model.QueryResult;
import vdt.se.demo.domain.model.SummaryResult;

import java.util.List;
import java.util.UUID;

public class QueryUseCaseService implements QueryUseCase {
    private final IqlSearchWorkflow searchWorkflow;
    private final QueryCsvExportService csvExportService;
    private final QueryHistoryPort queryHistoryPort;
    private final QuerySummaryService summaryService;
    private final String defaultUserId;

    public QueryUseCaseService(IqlSearchWorkflow searchWorkflow,
                               QueryCsvExportService csvExportService,
                               QueryHistoryPort queryHistoryPort,
                               QuerySummaryService summaryService,
                               String defaultUserId) {
        this.searchWorkflow = searchWorkflow;
        this.csvExportService = csvExportService;
        this.queryHistoryPort = queryHistoryPort;
        this.summaryService = summaryService;
        this.defaultUserId = defaultUserId;
    }

    @Override
    public QueryResult search(SearchRequest request) {
        return searchWorkflow.search(request);
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
