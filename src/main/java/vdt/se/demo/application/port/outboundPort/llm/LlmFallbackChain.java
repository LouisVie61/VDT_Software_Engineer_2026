package vdt.se.demo.application.port.outboundPort.llm;

import tools.jackson.databind.JsonNode;
import vdt.se.demo.application.dto.SearchRequest;
import vdt.se.demo.domain.model.ExecutionResult;

public interface LlmFallbackChain {
    LlmResponse generateDsl(SearchRequest request);

    String summarize(SearchRequest request, JsonNode generatedDsl, ExecutionResult executionResult);
}

