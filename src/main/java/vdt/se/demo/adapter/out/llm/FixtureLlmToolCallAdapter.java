package vdt.se.demo.adapter.out.llm;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.adapter.config.AppProperties;
import vdt.se.demo.application.port.outboundPort.llm.LlmToolCallPort;
import vdt.se.demo.domain.exception.LlmException;
import vdt.se.demo.domain.iql.SessionState;
import vdt.se.demo.domain.iql.ToolCallResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@ConditionalOnProperty(prefix = "app.llm", name = "mode", havingValue = "mock")
public final class FixtureLlmToolCallAdapter implements LlmToolCallPort {
    private final ToolCallResult fixture;

    public FixtureLlmToolCallAdapter(ResourceLoader resources, ObjectMapper mapper, AppProperties properties) {
        this.fixture = load(resources.getResource(properties.getLlm().getMockFixture()), new ToolCallResponseParser(mapper));
    }

    @Override
    public ToolCallResult invoke(String text, SessionState sessionState, List<JsonNode> toolDefinitions) {
        return fixture;
    }

    private ToolCallResult load(Resource resource, ToolCallResponseParser parser) {
        try (var input = resource.getInputStream()) {
            return parser.parse(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException failure) {
            throw new LlmException("Cannot load mock LLM fixture: " + resource.getDescription(), failure);
        }
    }
}
