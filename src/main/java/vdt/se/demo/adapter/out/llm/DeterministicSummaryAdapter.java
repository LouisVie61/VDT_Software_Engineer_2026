package vdt.se.demo.adapter.out.llm;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import vdt.se.demo.application.dto.SearchRequest;
import vdt.se.demo.application.port.outboundPort.llm.SummaryPort;
import vdt.se.demo.domain.model.ExecutionResult;

@Component
public final class DeterministicSummaryAdapter implements SummaryPort {
    private final DeterministicSummaryBuilder builder;
    public DeterministicSummaryAdapter(DeterministicSummaryBuilder builder) { this.builder = builder; }
    @Override public String summarize(SearchRequest request, JsonNode generatedDsl, ExecutionResult result) {
        return builder.build(result);
    }
}
