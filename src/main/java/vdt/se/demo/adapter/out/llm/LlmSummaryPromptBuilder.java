package vdt.se.demo.adapter.out.llm;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import vdt.se.demo.application.dto.SearchRequest;
import vdt.se.demo.domain.model.ExecutionResult;

@Component
public class LlmSummaryPromptBuilder {

    public String build(SearchRequest request, JsonNode generatedDsl, ExecutionResult result) {
        return """
                Summarize this SOC search result in 3-5 concise sentences for an analyst.
                Mention total events, notable users/hosts/IPs if present, and one investigation direction.
                Question: %s
                Generated DSL: %s
                Total count: %d
                Aggregations: %s
                Sample results: %s
                """.formatted(
                request.getQuestion(),
                generatedDsl,
                result.totalCount(),
                result.aggregations(),
                result.results().stream().limit(5).toList()
        );
    }
}
