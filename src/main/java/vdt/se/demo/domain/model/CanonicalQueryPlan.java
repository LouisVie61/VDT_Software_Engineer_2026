package vdt.se.demo.domain.model;

import lombok.Builder;
import vdt.se.demo.domain.valueObjects.ChartType;

import java.util.List;
import java.util.Map;

@Builder
public record CanonicalQueryPlan(
        String normalizedQuery,
        String schemaVersion,
        String sessionId,
        RoutingHint routingHint,
        SearchIntent extractedFields,
        SearchIntent mergedIntent,
        TemplateSelection templateSelection,
        ChartType chartHint,
        Map<String, Double> confidenceScores,
        String overrideIntent,
        String overrideReason,
        List<SemanticSpan> semanticSpans,
        List<SearchWarning> warnings,
        String provider,
        String rawLlmContent
) {
    public CanonicalQueryPlan {
        confidenceScores = confidenceScores == null ? Map.of() : Map.copyOf(confidenceScores);
        semanticSpans = semanticSpans == null ? List.of() : List.copyOf(semanticSpans);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
