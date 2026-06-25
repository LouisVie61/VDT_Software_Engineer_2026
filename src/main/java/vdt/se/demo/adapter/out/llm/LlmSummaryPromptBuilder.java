package vdt.se.demo.adapter.out.llm;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import vdt.se.demo.application.dto.SearchRequest;
import vdt.se.demo.domain.model.ExecutionResult;

@Component
public class LlmSummaryPromptBuilder {
    public String systemPrompt() {
        return """
                You are a SOC reporting assistant.
                Summarize only the supplied execution result. Do not use external knowledge.
                Focus on what an analyst can verify in the returned rows, aggregation buckets,
                counts, and generated query.
                """;
    }

    public String build(SearchRequest request, JsonNode generatedDsl, ExecutionResult result) {
        return """
                Summarize this SOC search result in 3-5 concise sentences for an analyst.
                The summary must:
                - state the total event count or bucket count
                - highlight notable returned fields such as severity, event type, user, host, or IP
                - mention the most visible pattern in the result sample or aggregation buckets
                - suggest the next investigation step, such as pivoting by user/host/IP, checking raw event details, or narrowing the time window
                Avoid inventing causes, attribution, or impact that is not present in the supplied data.

                Question: %s
                Generated DSL: %s
                Total count: %d
                Aggregations: %s
                Result sample: %s
                """.formatted(
                request.getQuestion(),
                generatedDsl,
                result.totalCount(),
                result.aggregations(),
                result.results().stream().limit(10).toList()
        );
    }
}
