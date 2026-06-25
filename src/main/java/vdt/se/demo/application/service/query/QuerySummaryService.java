package vdt.se.demo.application.service.query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import vdt.se.demo.application.dto.SearchRequest;
import vdt.se.demo.application.port.outboundPort.llm.SummaryPort;
import vdt.se.demo.domain.model.ExecutionResult;
import vdt.se.demo.domain.model.SummaryResult;
import vdt.se.demo.domain.valueObjects.ChartType;
import vdt.se.demo.domain.valueObjects.SummaryStatus;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

public class QuerySummaryService {
    private static final Logger log = LoggerFactory.getLogger(QuerySummaryService.class);

    private final SummaryPort summaryPort;
    private final Executor summaryTaskExecutor;
    private final Map<UUID, SummaryResult> results = new ConcurrentHashMap<>();

    public QuerySummaryService(SummaryPort summaryPort, Executor summaryTaskExecutor) {
        this.summaryPort = summaryPort;
        this.summaryTaskExecutor = summaryTaskExecutor;
    }

    public void schedule(UUID queryId, SearchRequest request, JsonNode generatedDsl, ExecutionResult executionResult,
                         ChartType chartType) {
        if (!hasSummaryPayload(executionResult)) {
            results.put(queryId, SummaryResult.builder()
                    .status(SummaryStatus.NOT_REQUIRED)
                    .summary("")
                    .chartType(chartType)
                    .build());
            return;
        }
        results.put(queryId, SummaryResult.builder()
                .status(SummaryStatus.PENDING)
                .summary("")
                .chartType(chartType)
                .build());
        CompletableFuture.runAsync(() -> {
            try {
                String summary = summaryPort.summarize(request, generatedDsl, executionResult);
                results.put(queryId, SummaryResult.builder()
                        .status(SummaryStatus.READY)
                        .summary(summary)
                        .chartType(chartType)
                        .build());
            } catch (RuntimeException e) {
                log.debug("Async summary failed: queryId={}, error={}", queryId, e.getMessage());
                results.put(queryId, SummaryResult.builder()
                        .status(SummaryStatus.FAILED)
                        .summary("Summary generation failed; Elasticsearch results remain available.")
                        .chartType(chartType)
                        .build());
            }
        }, summaryTaskExecutor);
    }

    private boolean hasSummaryPayload(ExecutionResult executionResult) {
        return !executionResult.results().isEmpty() || !executionResult.aggregations().isEmpty();
    }

    public SummaryResult find(UUID queryId) {
        return results.getOrDefault(queryId, SummaryResult.builder()
                .status(SummaryStatus.FAILED)
                .summary("Summary is not available for this query.")
                .chartType(ChartType.TABLE)
                .build());
    }
}
