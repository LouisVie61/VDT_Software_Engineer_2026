package vdt.se.demo.domain.model;

import lombok.Builder;

import java.util.List;

@Builder
public record ZeroResultDiagnostic(
        boolean originalResultTrusted,
        String reasonCode,
        String field,
        List<DiagnosticProbeResult> probeResults,
        List<SuggestedRelaxation> suggestedRelaxations
) {
    public ZeroResultDiagnostic {
        probeResults = probeResults == null ? List.of() : List.copyOf(probeResults);
        suggestedRelaxations = suggestedRelaxations == null ? List.of() : List.copyOf(suggestedRelaxations);
    }
}
