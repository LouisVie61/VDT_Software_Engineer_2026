package vdt.se.demo.application.service.routing;

import vdt.se.demo.domain.model.RoutingHint;
import vdt.se.demo.domain.model.SearchIntent;

public class RoutingHintPolicy {
    public RoutingHint hintForLlm(QueryRoutingService.RoutingDecision decision, SearchIntent previousIntent) {
        if (decision == null) {
            return RoutingHint.neutralLowConfidence("no routing signals available");
        }
        if (decision.bothLowConfidence() && previousIntent == null) {
            return RoutingHint.neutralLowConfidence(
                    "low-signal cold start: heuristic and semantic extractors found insufficient evidence");
        }
        if (decision.bothLowConfidence()) {
            return RoutingHint.builder()
                    .templateType(previousIntent == null ? null : previousIntent.getIntent())
                    .confidence(0.50d)
                    .reason("short low-confidence follow-up: previous session intent supplied as context")
                    .build();
        }
        return decision.strongestSignal();
    }
}
