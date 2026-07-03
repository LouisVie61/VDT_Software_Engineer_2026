package vdt.se.demo.application.port.outboundPort.execution;

import tools.jackson.databind.JsonNode;
import vdt.se.demo.application.dto.QueryExecution;
import vdt.se.demo.application.dto.SearchRequest;
import vdt.se.demo.domain.model.ExecutionResult;

public interface QueryExecutionRefiner {

    QueryExecution refine(SearchRequest request, JsonNode generatedDsl, ExecutionResult executionResult) throws Exception;
}

