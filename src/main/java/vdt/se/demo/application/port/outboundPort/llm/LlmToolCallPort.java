package vdt.se.demo.application.port.outboundPort.llm;

import tools.jackson.databind.JsonNode;
import vdt.se.demo.domain.iql.SessionState;
import vdt.se.demo.domain.iql.ToolCallResult;

import java.util.List;

public interface LlmToolCallPort {
    ToolCallResult invoke(String naturalLanguageText, SessionState sessionState, List<JsonNode> toolDefinitions);
}
