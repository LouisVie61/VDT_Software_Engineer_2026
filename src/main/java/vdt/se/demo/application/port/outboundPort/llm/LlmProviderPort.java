package vdt.se.demo.application.port.outboundPort.llm;

import vdt.se.demo.domain.valueObjects.LlmProvider;
import tools.jackson.databind.JsonNode;
import java.util.List;

public interface LlmProviderPort {
    LlmProvider provider();

    String complete(String prompt);

    default String complete(String systemPrompt, String userPrompt) {
        if (systemPrompt == null || systemPrompt.isBlank()) {
            return complete(userPrompt);
        }
        return complete(systemPrompt + "\n\n" + userPrompt);
    }

    /** Native provider tool/function calling. Legacy providers may fall back to constrained JSON text. */
    default String completeWithTools(String systemPrompt, String userPrompt, List<JsonNode> toolDefinitions) {
        return complete(systemPrompt, userPrompt);
    }

    default String completeWithTools(String systemPrompt, String userPrompt, List<JsonNode> toolDefinitions,
                                     String queryText, boolean forceComplexModel) {
        return completeWithTools(systemPrompt, userPrompt, toolDefinitions);
    }
}

