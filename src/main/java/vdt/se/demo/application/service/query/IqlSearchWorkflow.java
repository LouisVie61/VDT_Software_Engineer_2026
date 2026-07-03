package vdt.se.demo.application.service.query;

import vdt.se.demo.application.dto.SearchRequest;
import vdt.se.demo.application.port.outboundPort.session.SessionStateStore;
import vdt.se.demo.application.service.execution.CachedIqlExecutionService;
import vdt.se.demo.domain.iql.IqlQuery;
import vdt.se.demo.domain.iql.ResultSummary;
import vdt.se.demo.domain.iql.SessionState;
import vdt.se.demo.domain.model.ExecutionResult;
import vdt.se.demo.domain.model.QueryResult;
import vdt.se.demo.domain.valueObjects.ChartType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class IqlSearchWorkflow {
    private final SessionStateStore states; private final IqlQueryPreparationService preparation;
    private final CachedIqlExecutionService execution;

    public IqlSearchWorkflow(SessionStateStore states, IqlQueryPreparationService preparation,
                             CachedIqlExecutionService execution) {
        this.states=states; this.preparation=preparation; this.execution=execution;
    }

    public QueryResult search(SearchRequest request) {
        String sessionId = request.getSessionId() == null || request.getSessionId().isBlank() ? "soc-analyst-demo" : request.getSessionId();
        SessionState previous = states.load(sessionId).orElse(new SessionState(sessionId, null, null, Instant.EPOCH));
        IqlQuery query = preparation.prepare(request.getQuestion(), previous);
        CachedIqlExecutionService.Executed executed = execution.execute(query);
        ExecutionResult result = executed.result();
        ResultSummary summary = new ResultSummary(result.totalCount(), query.timeRange(), List.of(), null);
        states.save(sessionId, new SessionState(sessionId, query, summary, Instant.now()));
        return QueryResult.builder().id(UUID.randomUUID()).nlQuery(request.getQuestion()).generatedDSL(executed.dsl())
                .results(result.results()).aggregations(result.aggregations()).totalCount(result.totalCount())
                .chartType(query.groupBy().isEmpty() ? ChartType.TABLE : ChartType.BAR_CHART)
                .page(request.getPage()).pageSize(request.getPageSize()).selectedTemplate("IQL")
                .warnings(result.warnings()).build();
    }
}
