package vdt.se.demo.application.dto;

import tools.jackson.databind.JsonNode;
import vdt.se.demo.domain.model.ExecutionResult;

public record QueryExecution(JsonNode generatedDsl, ExecutionResult executionResult) {
}
