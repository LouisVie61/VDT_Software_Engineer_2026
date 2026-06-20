package vdt.se.demo.application.dto;

import lombok.Builder;
import vdt.se.demo.domain.model.RoutingHint;
import vdt.se.demo.domain.model.SearchIntent;

import java.util.List;

@Builder
public record IntentExtractionRequest(
        SearchRequest request,
        RoutingHint routingHint,
        RoutingHint heuristicHint,
        RoutingHint semanticHint,
        SearchIntent previousIntent,
        List<String> enrichments
) {
}
