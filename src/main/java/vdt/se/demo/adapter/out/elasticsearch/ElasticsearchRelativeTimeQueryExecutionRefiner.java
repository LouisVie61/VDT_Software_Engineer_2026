package vdt.se.demo.adapter.out.elasticsearch;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import vdt.se.demo.application.dto.QueryExecution;
import vdt.se.demo.application.dto.SearchRequest;
import vdt.se.demo.application.port.outboundPort.QueryExecutionRefiner;
import vdt.se.demo.application.port.outboundPort.QueryExecutorPort;
import vdt.se.demo.domain.model.ExecutionResult;
import vdt.se.demo.domain.service.RelativeTimeWindowParser;
import vdt.se.demo.domain.valueObjects.RelativeTimeWindow;

import java.time.Instant;
import java.util.Optional;

@Component
public class ElasticsearchRelativeTimeQueryExecutionRefiner implements QueryExecutionRefiner {

    private final QueryExecutorPort queryExecutorPort;
    private final RelativeTimeWindowParser relativeTimeWindowParser;
    private final ElasticsearchDslTimeRangeEditor timeRangeEditor;
    private final ElasticsearchLatestTimestampResolver latestTimestampResolver;

    public ElasticsearchRelativeTimeQueryExecutionRefiner(QueryExecutorPort queryExecutorPort,
                                                          RelativeTimeWindowParser relativeTimeWindowParser,
                                                          ElasticsearchDslTimeRangeEditor timeRangeEditor,
                                                          ElasticsearchLatestTimestampResolver latestTimestampResolver) {
        this.queryExecutorPort = queryExecutorPort;
        this.relativeTimeWindowParser = relativeTimeWindowParser;
        this.timeRangeEditor = timeRangeEditor;
        this.latestTimestampResolver = latestTimestampResolver;
    }

    @Override
    public QueryExecution refine(SearchRequest request, JsonNode generatedDsl, ExecutionResult executionResult)
            throws Exception {
        Optional<RelativeTimeWindow> relativeWindow = relativeWindow(request, generatedDsl, executionResult);
        if (relativeWindow.isEmpty()) {
            return new QueryExecution(generatedDsl, executionResult);
        }

        JsonNode baseDsl = timeRangeEditor.withoutTimestampRange(generatedDsl);
        Instant latestTimestamp = latestTimestampResolver.resolve(baseDsl).orElse(null);
        if (latestTimestamp == null) {
            return new QueryExecution(generatedDsl, executionResult);
        }

        Instant from = latestTimestamp.minus(relativeWindow.get().duration());
        JsonNode rewrittenDsl = timeRangeEditor.withTimestampRange(generatedDsl, from, latestTimestamp);
        return new QueryExecution(rewrittenDsl, queryExecutorPort.execute(rewrittenDsl));
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

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
