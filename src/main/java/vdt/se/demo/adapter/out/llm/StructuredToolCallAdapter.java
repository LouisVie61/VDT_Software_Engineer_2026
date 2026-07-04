package vdt.se.demo.adapter.out.llm;

import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.application.port.outboundPort.llm.LlmProviderPort;
import vdt.se.demo.application.port.outboundPort.llm.LlmCallBudget;
import vdt.se.demo.application.port.outboundPort.llm.LlmToolCallPort;
import vdt.se.demo.adapter.config.AppProperties;
import vdt.se.demo.domain.iql.SessionState;
import vdt.se.demo.domain.iql.ToolCallResult;

import java.util.List;

@Component
@ConditionalOnProperty(prefix = "app.llm", name = "mode", havingValue = "real", matchIfMissing = true)
public final class StructuredToolCallAdapter implements LlmToolCallPort {
    private static final Logger log = LoggerFactory.getLogger(StructuredToolCallAdapter.class);
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
        return invoke(text, state, definitions, List.of(), new LlmCallBudget(2));
    }

    @Override
    public ToolCallResult invoke(String text, SessionState state, List<JsonNode> definitions,
                                 List<String> correctionErrors, LlmCallBudget budget) {
        String system = prompts.systemPrompt(definitions);
        String user = prompts.userPrompt(text, state, correctionErrors);
        log.info("LLM_INPUT requestId={} text={} correctionErrors={}", requestId(), text, correctionErrors);
        return chain.firstSuccessful(provider -> {
            boolean forceComplex = correctionErrors.stream().anyMatch("EMPTY_DSL"::equals);
            String routingText = text.split("\\n\\nAuthoritative structured constraints", 2)[0];
            String raw = provider.completeWithTools(system, user, definitions, routingText, forceComplex);
            log.info("LLM_OUTPUT requestId={} provider={} output={}", requestId(), provider.provider(), raw);
            return parser.parse(raw);
        }, budget);
    }

    private String requestId() {
        String value = MDC.get("requestId");
        return value == null || value.isBlank() ? "n/a" : value;
    }
}
