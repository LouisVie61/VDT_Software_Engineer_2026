package vdt.se.demo.application.port.outboundPort.execution;

import tools.jackson.databind.JsonNode;
import vdt.se.demo.application.dto.SearchRequest;
import vdt.se.demo.domain.model.CanonicalQueryPlan;

public interface SearchDslBuilderPort {
    JsonNode build(SearchRequest request, CanonicalQueryPlan plan);
}

