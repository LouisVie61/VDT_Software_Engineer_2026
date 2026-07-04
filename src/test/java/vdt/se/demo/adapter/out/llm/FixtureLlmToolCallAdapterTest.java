package vdt.se.demo.adapter.out.llm;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.adapter.config.AppProperties;
import vdt.se.demo.domain.iql.ToolCallResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FixtureLlmToolCallAdapterTest {
    @Test
    void returnsDeterministicToolCallWithoutAProvider() {
        AppProperties properties = new AppProperties();
        properties.getLlm().setMockFixture("classpath:llm/mock-search-events.json");
        var adapter = new FixtureLlmToolCallAdapter(new DefaultResourceLoader(), new ObjectMapper(), properties);

        ToolCallResult first = adapter.invoke("first text", null, List.of());
        ToolCallResult second = adapter.invoke("different text", null, List.of());

        assertThat(first).isEqualTo(second).isInstanceOf(ToolCallResult.SearchEvents.class);
    }
}
