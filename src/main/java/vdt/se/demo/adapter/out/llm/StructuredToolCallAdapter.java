package vdt.se.demo.adapter.out.llm;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.application.port.outboundPort.llm.LlmProviderPort;
import vdt.se.demo.application.port.outboundPort.llm.LlmToolCallPort;
import vdt.se.demo.adapter.config.AppProperties;
import vdt.se.demo.domain.iql.SessionState;
import vdt.se.demo.domain.iql.ToolCallResult;

import java.util.List;

@Component
public final class StructuredToolCallAdapter implements LlmToolCallPort {
    private final LlmProviderChain chain;
    private final IqlSystemPromptBuilder prompts;
    private final ToolCallResponseParser parser;

    public StructuredToolCallAdapter(List<LlmProviderPort> providers, ObjectMapper mapper, AppProperties properties) {
        this.chain = new LlmProviderChain(providers, properties);
        this.prompts = new IqlSystemPromptBuilder(mapper);
        this.parser = new ToolCallResponseParser(mapper);
    }

    @Override
    public ToolCallResult invoke(String text, SessionState state, List<JsonNode> definitions) {
        String system = prompts.systemPrompt(definitions);
        String user = prompts.userPrompt(text, state);
        return chain.firstSuccessful(provider -> parser.parse(provider.complete(system, user)));
    }
}
