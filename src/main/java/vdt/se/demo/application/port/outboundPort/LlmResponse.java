package vdt.se.demo.application.port.outboundPort;

import tools.jackson.databind.JsonNode;

public record LlmResponse(String provider, JsonNode generatedDsl, String rawContent) {
}
