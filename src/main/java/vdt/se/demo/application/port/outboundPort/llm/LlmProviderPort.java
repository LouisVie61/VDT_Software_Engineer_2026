package vdt.se.demo.application.port.outboundPort.llm;

import vdt.se.demo.domain.valueObjects.LlmProvider;

public interface LlmProviderPort {
    LlmProvider provider();

    String complete(String prompt);

    default String complete(String systemPrompt, String userPrompt) {
        if (systemPrompt == null || systemPrompt.isBlank()) {
            return complete(userPrompt);
        }
        return complete(systemPrompt + "\n\n" + userPrompt);
    }
}

