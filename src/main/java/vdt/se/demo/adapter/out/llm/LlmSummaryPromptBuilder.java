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
                Summarize only the supplied execution result. Do not infer facts from raw rows,
                routing hints, or external knowledge.
                """;
    }

    public String build(SearchRequest request, JsonNode generatedDsl, ExecutionResult result) {
        return """
                Summarize this SOC search result in 3-5 concise sentences for an analyst.
                Confirm the chart type when aggregation buckets are present.
                Use only aggregation buckets and metadata. Do not infer from raw event rows.
                Question: %s
                Generated DSL: %s
                Total count: %d
                Aggregations: %s
                """.formatted(
                request.getQuestion(),
                generatedDsl,
                result.totalCount(),
                result.aggregations()
        );
    }
}
