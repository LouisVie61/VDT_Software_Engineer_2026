package vdt.se.demo.adapter.out.llm;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.adapter.config.AppProperties;
import vdt.se.demo.application.port.outboundPort.llm.LlmProviderPort;
import vdt.se.demo.domain.iql.ToolCallResult;
import vdt.se.demo.domain.valueObjects.LlmProvider;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StructuredToolCallAdapterTest {
    @Test
    void followsConfiguredProviderOrder() {
        List<LlmProvider> calls = new ArrayList<>();
        AppProperties properties = new AppProperties();
        properties.getLlm().setProviderOrder("GROQ,GEMINI");
        LlmProviderPort gemini = provider(LlmProvider.GEMINI, calls);
        LlmProviderPort groq = provider(LlmProvider.GROQ, calls);
        StructuredToolCallAdapter adapter = new StructuredToolCallAdapter(List.of(gemini, groq), new ObjectMapper(), properties);

        ToolCallResult result = adapter.invoke("show events", null, List.of());

        assertThat(result).isInstanceOf(ToolCallResult.AskClarification.class);
        assertThat(calls).containsExactly(LlmProvider.GROQ);
    }

    private LlmProviderPort provider(LlmProvider type, List<LlmProvider> calls) {
        return new LlmProviderPort() {
            public LlmProvider provider() { return type; }
            public String complete(String prompt) { return complete(null, prompt); }
            public String complete(String system, String user) {
                calls.add(type);
                assertThat(system).contains("Never output prose", "Elasticsearch DSL", "Pagination is server-managed");
                return "{\"name\":\"ask_clarification\",\"arguments\":{\"reason\":\"unclear_intent\",\"question\":\"Clarify\"}}";
            }
        };
    }
}
