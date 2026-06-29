package vdt.se.demo.adapter.out.elasticsearch.refine;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import vdt.se.demo.adapter.out.elasticsearch.dsl.ElasticsearchDslTimeRangeEditor;
import vdt.se.demo.adapter.out.elasticsearch.dsl.ElasticsearchExplicitFilterDslEditor;
import vdt.se.demo.application.dto.QueryExecution;
import vdt.se.demo.application.dto.SearchRequest;
import vdt.se.demo.application.port.outboundPort.execution.QueryExecutionRefiner;
import vdt.se.demo.application.port.outboundPort.execution.QueryExecutorPort;
import vdt.se.demo.domain.model.ExecutionResult;
import vdt.se.demo.domain.model.SearchWarning;
import vdt.se.demo.domain.service.RelativeTimeWindowParser;
import vdt.se.demo.domain.valueObjects.RelativeTimeWindow;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class ElasticsearchRelativeTimeQueryExecutionRefiner implements QueryExecutionRefiner {

    private final QueryExecutorPort queryExecutorPort;
    private final ElasticsearchExplicitFilterDslEditor explicitFilterDslEditor;
    private final RelativeTimeWindowParser relativeTimeWindowParser;
    private final ElasticsearchDslTimeRangeEditor timeRangeEditor;
    private final ElasticsearchLatestTimestampResolver latestTimestampResolver;

    public ElasticsearchRelativeTimeQueryExecutionRefiner(QueryExecutorPort queryExecutorPort,
                                                          ElasticsearchExplicitFilterDslEditor explicitFilterDslEditor,
                                                          RelativeTimeWindowParser relativeTimeWindowParser,
                                                          ElasticsearchDslTimeRangeEditor timeRangeEditor,
                                                          ElasticsearchLatestTimestampResolver latestTimestampResolver) {
        this.queryExecutorPort = queryExecutorPort;
        this.explicitFilterDslEditor = explicitFilterDslEditor;
        this.relativeTimeWindowParser = relativeTimeWindowParser;
        this.timeRangeEditor = timeRangeEditor;
        this.latestTimestampResolver = latestTimestampResolver;
    }

    @Override
    public QueryExecution refine(SearchRequest request, JsonNode generatedDsl, ExecutionResult executionResult)
            throws Exception {
        JsonNode filteredDsl = explicitFilterDslEditor.withExplicitFilters(request, generatedDsl);
        ExecutionResult filteredExecutionResult = executionResult;
        if (!filteredDsl.toString().equals(generatedDsl.toString())) {
            filteredExecutionResult = queryExecutorPort.execute(filteredDsl);
        }

        Optional<RelativeTimeWindow> relativeWindow = relativeWindow(request, filteredDsl, filteredExecutionResult);
        if (relativeWindow.isEmpty()) {
            return new QueryExecution(filteredDsl, filteredExecutionResult);
        }

        JsonNode baseDsl = timeRangeEditor.withoutTimestampRange(filteredDsl);
        Instant latestTimestamp = latestTimestampResolver.resolve(baseDsl).orElse(null);
        if (latestTimestamp == null) {
            return new QueryExecution(filteredDsl, filteredExecutionResult);
        }

        return new QueryExecution(filteredDsl, withLatestDataWarning(filteredExecutionResult, latestTimestamp));
    }

    private Optional<RelativeTimeWindow> relativeWindow(SearchRequest request, JsonNode generatedDsl,
                                                       ExecutionResult executionResult) {
        if (executionResult.totalCount() != 0 || hasExplicitTimeBounds(request) || !timeRangeEditor.containsNow(generatedDsl)) {
            return Optional.empty();
        }
        return relativeTimeWindowParser.parse(request.getQuestion());
    }

    private boolean hasExplicitTimeBounds(SearchRequest request) {
        return hasText(request.getFrom()) || hasText(request.getTo());
    }

    private ExecutionResult withLatestDataWarning(ExecutionResult executionResult, Instant latestTimestamp) {
        List<SearchWarning> warnings = new ArrayList<>(executionResult.warnings());
        warnings.add(SearchWarning.builder()
                .code("RELATIVE_TIME_WINDOW_EMPTY")
                .message("No data matched the requested relative time window. The latest matching data timestamp is "
                        + latestTimestamp + ". Review the zero-result diagnostic before widening the time range.")
                .build());
        return new ExecutionResult(
                executionResult.results(),
                executionResult.aggregations(),
                executionResult.totalCount(),
                warnings
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
