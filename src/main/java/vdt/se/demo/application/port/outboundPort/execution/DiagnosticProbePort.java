package vdt.se.demo.application.port.outboundPort.execution;

import tools.jackson.databind.JsonNode;

public interface DiagnosticProbePort {
    int count(JsonNode countDsl);
}
