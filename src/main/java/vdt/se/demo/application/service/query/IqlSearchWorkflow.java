package vdt.se.demo.application.service.query;

import vdt.se.demo.application.dto.SearchRequest;
import vdt.se.demo.application.port.outboundPort.session.SessionStateStore;
import vdt.se.demo.application.service.execution.CachedIqlExecutionService;
import vdt.se.demo.domain.iql.IqlQuery;
import vdt.se.demo.domain.iql.ResultSummary;
import vdt.se.demo.domain.iql.SessionState;
import vdt.se.demo.domain.iql.SearchConstraints;
import vdt.se.demo.domain.model.ExecutionResult;
import vdt.se.demo.domain.model.QueryResult;
import vdt.se.demo.domain.valueObjects.ChartType;

import java.time.Instant;
import java.util.UUID;

public final class IqlSearchWorkflow {
    private final SessionStateStore states; private final IqlQueryPreparationService preparation;
    private final CachedIqlExecutionService execution;
    private final ResultSummaryBuilder summaries;

    public IqlSearchWorkflow(SessionStateStore states, IqlQueryPreparationService preparation,
                             CachedIqlExecutionService execution, ResultSummaryBuilder summaries) {
        this.states=states; this.preparation=preparation; this.execution=execution; this.summaries=summaries;
    }

    public QueryResult search(SearchRequest request) {
        String sessionId = request.getSessionId() == null || request.getSessionId().isBlank() ? "soc-analyst-demo" : request.getSessionId();
        SessionState previous = states.load(sessionId).orElse(new SessionState(sessionId, null, null, Instant.EPOCH));
        SearchConstraints constraints = new SearchConstraints(request.getFrom(), request.getTo(), request.getSeverity(),
                request.getEventType(), request.getUser(), request.getHost(), request.getIp());
        IqlQuery query = preparation.prepare(request.getQuestion(), previous, constraints);
        CachedIqlExecutionService.Executed executed = execution.execute(query, request.getPage(), request.getPageSize(), request.getSearchAfter());
        ExecutionResult result = executed.result();
        ResultSummary summary = summaries.build(query, result);
        states.save(sessionId, new SessionState(sessionId, query, summary, Instant.now()));
        return QueryResult.builder().id(UUID.randomUUID()).nlQuery(request.getQuestion()).generatedDSL(executed.dsl())
                .results(result.results()).aggregations(result.aggregations()).totalCount(result.totalCount())
                .chartType(query.groupBy().isEmpty() ? ChartType.TABLE : ChartType.BAR_CHART)
                .page(request.getPage()).pageSize(request.getPageSize()).selectedTemplate("IQL")
                .cacheHit(executed.cacheHit())
                .warnings(result.warnings()).build();
    }
}
