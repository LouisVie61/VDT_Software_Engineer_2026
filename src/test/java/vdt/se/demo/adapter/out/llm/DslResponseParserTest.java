package vdt.se.demo.adapter.out.llm;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.domain.exception.BadQueryException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DslResponseParserTest {

    private final DslResponseParser parser = new DslResponseParser(new ObjectMapper());

    @Test
    void parsesDslObject() {
        JsonNode dsl = parser.parse("""
                {"query":{"match_all":{}},"size":10}
                """);

        assertThat(dsl.has("query")).isTrue();
        assertThat(dsl.get("size").asInt()).isEqualTo(10);
    }

    @Test
    void stripsMarkdownFence() {
        JsonNode dsl = parser.parse("""
                ```json
                {"query":{"match_all":{}}}
                ```
                """);

        assertThat(dsl.has("query")).isTrue();
    }

    @Test
    void rejectsInvalidJson() {
        assertThatThrownBy(() -> parser.parse("not-json"))
                .isInstanceOf(BadQueryException.class)
                .hasMessageContaining("invalid Elasticsearch DSL JSON");
    }

    @Test
    void rejectsNonObjectJson() {
        assertThatThrownBy(() -> parser.parse("[{\"query\":{\"match_all\":{}}}]"))
                .isInstanceOf(BadQueryException.class)
                .hasMessageContaining("not a JSON object");
    }
}
