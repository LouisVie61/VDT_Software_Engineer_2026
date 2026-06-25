package vdt.se.demo.application.service.query;

import vdt.se.demo.domain.model.DiagnosticProbeResult;
import vdt.se.demo.domain.model.SuggestedRelaxation;
import vdt.se.demo.domain.model.ZeroResultDiagnostic;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ZeroResultDiagnosticClassifier {
    public ZeroResultDiagnostic classify(List<DiagnosticProbeResult> probes) {
        int p1 = count(probes, "P1");
        int p3 = count(probes, "P3");
        int p4 = count(probes, "P4");
        int p5 = count(probes, "P5");
        DiagnosticReason reason = reason(p1, p3, p4, p5);
        List<SuggestedRelaxation> suggestions = suggestions(reason, p3, p4, p5);
        return ZeroResultDiagnostic.builder()
                .originalResultTrusted(true)
                .reasonCode(reason.code)
                .field(reason.field)
                .probeResults(probes)
                .suggestedRelaxations(suggestions)
                .build();
    }

    private DiagnosticReason reason(int p1, int p3, int p4, int p5) {
        if (p1 == 0) {
            return new DiagnosticReason("NO_DATA_IN_TIME_RANGE", "timestamp");
        }
        if (p3 > 0) {
            return new DiagnosticReason("LOW_CONFIDENCE_FILTER_RELAXATION_FOUND", "low_confidence_filters");
        }
        if (p4 > 0) {
            return new DiagnosticReason("TEXT_TERMS_MAY_BE_TOO_RESTRICTIVE", "textQuery");
        }
        if (p5 > 0 || p1 > 0) {
            return new DiagnosticReason("FILTERS_MAY_BE_TOO_RESTRICTIVE", p5 > 0 ? "timestamp" : null);
        }
        return new DiagnosticReason("NO_RELAXATION_RECOVERED", null);
    }

    private List<SuggestedRelaxation> suggestions(DiagnosticReason reason, int p3, int p4, int p5) {
        List<SuggestedRelaxation> suggestions = new ArrayList<>();
        addSuggestion(suggestions, reason, "low_confidence_filters", p3);
        addSuggestion(suggestions, reason, "textQuery", p4);
        addSuggestion(suggestions, reason, "timestamp", p5);
        suggestions.sort(Comparator.comparingInt(SuggestedRelaxation::previewCount).reversed());
        return suggestions;
    }

    private void addSuggestion(List<SuggestedRelaxation> suggestions, DiagnosticReason reason,
                               String field, int count) {
        if (count <= 0 || !field.equals(reason.field)) {
            return;
        }
        suggestions.add(SuggestedRelaxation.builder()
                .action("VIEW_RELAXED_RESULTS")
                .field(field)
                .reasonCode(reason.code)
                .previewCount(count)
                .build());
    }

    private int count(List<DiagnosticProbeResult> probes, String name) {
        return probes.stream()
                .filter(probe -> name.equals(probe.probe()))
                .findFirst()
                .map(DiagnosticProbeResult::count)
                .orElse(0);
    }

    private record DiagnosticReason(String code, String field) {
    }
}
