package vdt.se.demo.application.port.outboundPort.llm;

import tools.jackson.databind.JsonNode;

public record LlmResponse(String provider, JsonNode generatedDsl, String rawContent) {
}

