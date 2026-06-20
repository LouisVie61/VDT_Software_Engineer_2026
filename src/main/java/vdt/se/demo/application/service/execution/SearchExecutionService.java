package vdt.se.demo.application.service.execution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import vdt.se.demo.application.dto.QueryExecution;
import vdt.se.demo.application.dto.SearchRequest;
import vdt.se.demo.application.port.outboundPort.execution.QueryExecutionRefiner;
import vdt.se.demo.application.port.outboundPort.execution.QueryExecutorPort;
import vdt.se.demo.application.port.outboundPort.execution.SearchDslBuilderPort;
import vdt.se.demo.domain.model.CanonicalQueryPlan;
import vdt.se.demo.domain.model.ExecutionResult;
import vdt.se.demo.domain.model.QueryResult;
import vdt.se.demo.domain.model.SearchWarning;
import vdt.se.demo.domain.service.ChartTypeInferenceService;
import vdt.se.demo.domain.valueObjects.ChartType;
import vdt.se.demo.domain.valueObjects.SummaryStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

public class SearchExecutionService {
    private static final Logger log = LoggerFactory.getLogger(SearchExecutionService.class);

    private final SearchDslBuilderPort dslBuilderPort;
    private final QueryExecutorPort queryExecutorPort;
    private final QueryExecutionRefiner queryExecutionRefiner;
    private final ChartTypeInferenceService chartTypeInferenceService;

    public SearchExecutionService(SearchDslBuilderPort dslBuilderPort, QueryExecutorPort queryExecutorPort,
                                  QueryExecutionRefiner queryExecutionRefiner,
                                  ChartTypeInferenceService chartTypeInferenceService) {
        this.dslBuilderPort = dslBuilderPort;
        this.queryExecutorPort = queryExecutorPort;
        this.queryExecutionRefiner = queryExecutionRefiner;
        this.chartTypeInferenceService = chartTypeInferenceService;
    }

    public ExecutedSearch execute(UUID queryId, SearchRequest request, CanonicalQueryPlan plan) throws Exception {
        JsonNode generatedDsl = dslBuilderPort.build(request, plan);
        return executeDsl(queryId, request, generatedDsl, plan);
    }

    public ExecutedSearch executeDsl(UUID queryId, SearchRequest request, JsonNode generatedDsl,
                                     CanonicalQueryPlan plan) throws Exception {
        long startedAt = System.nanoTime();
        ExecutionResult executionResult = queryExecutorPort.execute(generatedDsl);
        logStage(queryId, "elasticsearch.primary", startedAt);

        startedAt = System.nanoTime();
        QueryExecution refinedExecution = queryExecutionRefiner.refine(request, generatedDsl, executionResult);
        logStage(queryId, "execution.refine", startedAt);

        generatedDsl = refinedExecution.generatedDsl();
        executionResult = refinedExecution.executionResult();

        ChartType chartType = plan.chartHint() == null ? chartTypeInferenceService.inferChartType(generatedDsl) : plan.chartHint();
        List<SearchWarning> warnings = new ArrayList<>();
        warnings.addAll(plan.warnings());
        warnings.addAll(executionResult.warnings());
        QueryResult result = QueryResult.builder()
                .id(queryId)
                .nlQuery(request.getQuestion())
                .generatedDSL(generatedDsl)
                .summary("")
                .results(executionResult.results())
                .aggregations(executionResult.aggregations())
                .totalCount(executionResult.totalCount())
                .chartType(chartType)
                .page(request.getPage())
                .pageSize(request.getPageSize())
                .summaryStatus(executionResult.aggregations().isEmpty() ? SummaryStatus.NOT_REQUIRED : SummaryStatus.PENDING)
                .overrideIntent(plan.overrideIntent())
                .overrideReason(plan.overrideReason())
                .confidenceScores(plan.confidenceScores())
                .canonicalPlanId(queryId)
                .build();
        result.setWarnings(warnings);
        result.setSelectedTemplate(plan.templateSelection().type().name());
        return new ExecutedSearch(result, generatedDsl, executionResult);
    }

    private void logStage(UUID queryId, String stage, long startedAt) {
        log.debug("Search stage completed: queryId={}, stage={}, elapsedMs={}",
                queryId, stage, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt));
    }

    public record ExecutedSearch(QueryResult result, JsonNode dsl, ExecutionResult executionResult) {
    }
}

