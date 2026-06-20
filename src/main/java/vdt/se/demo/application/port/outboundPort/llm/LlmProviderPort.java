package vdt.se.demo.application.port.outboundPort.llm;

import vdt.se.demo.domain.valueObjects.LlmProvider;

public interface LlmProviderPort {
    LlmProvider provider();

    String complete(String prompt);
}

