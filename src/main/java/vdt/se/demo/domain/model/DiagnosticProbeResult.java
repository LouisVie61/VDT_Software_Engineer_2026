package vdt.se.demo.domain.model;

import lombok.Builder;

@Builder
public record DiagnosticProbeResult(
        String probe,
        int count,
        String description
) {
}
