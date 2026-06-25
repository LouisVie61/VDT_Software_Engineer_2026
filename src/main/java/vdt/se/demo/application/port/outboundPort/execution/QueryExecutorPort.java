package vdt.se.demo.application.port.outboundPort.execution;

import tools.jackson.databind.JsonNode;
import vdt.se.demo.domain.model.ExecutionResult;

public interface QueryExecutorPort {
    ExecutionResult execute(JsonNode generatedDsl);
}

