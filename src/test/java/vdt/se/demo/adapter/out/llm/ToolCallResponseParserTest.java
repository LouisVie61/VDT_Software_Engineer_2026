package vdt.se.demo.adapter.out.llm;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.application.service.llm.LlmToolDefinitions;
import vdt.se.demo.domain.iql.IqlQuery;
import vdt.se.demo.domain.iql.ToolCallResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ToolCallResponseParserTest {
    @Test
    void toolDefinitionsDescribeSearchAndClarificationInEnglishAndVietnamese() {
        LlmToolDefinitions definitions = new LlmToolDefinitions(new ObjectMapper());

        assertThat(definitions.searchEvents().path("description").asString())
                .contains("EN:", "VI:", "Tìm kiếm");
        assertThat(definitions.askClarification().path("description").asString())
                .contains("EN:", "VI:", "thực sự còn thiếu");
    }

    @Test
    void treatsEmptyOptionalOrderByAsAbsent() {
        ToolCallResult.SearchEvents call = (ToolCallResult.SearchEvents) new ToolCallResponseParser(new ObjectMapper()).parse("""
                {"name":"search_events","arguments":{"mode":"new","filters":[],"group_by":[],
                "metrics":[{"type":"count"}],"sort":[],"order_by":{},"size":50}}
                """);

        assertThat(call.query().orderBy()).isNull();
    }

    @Test
    void toolSchemaDefinesOrderByContract() {
        JsonNode orderBy = new vdt.se.demo.application.service.llm.LlmToolDefinitions(new ObjectMapper())
                .searchEvents().path("input_schema").path("properties").path("order_by");

        assertThat(orderBy.path("required").toString()).contains("target", "direction");
        assertThat(orderBy.path("properties").path("target").path("enum").toString()).contains("metric", "key");
    }


    private final ToolCallResponseParser parser = new ToolCallResponseParser(new ObjectMapper());

    @Test
    void parsesLowercaseSnakeCaseIqlContract() {
        ToolCallResult.SearchEvents call = (ToolCallResult.SearchEvents) parser.parse("""
                {"name":"search_events","arguments":{"mode":"new","filters":[
                  {"id":"f1","field":"severity","op":"eq","value":"critical"}],
                  "group_by":[{"field":"source","size":10}],
                  "metrics":[{"type":"cardinality","field":"host"}],
                  "order_by":{"target":"metric","metric_index":0,"direction":"desc"},"size":50}}
                """);

        assertThat(call.mode()).isEqualTo(ToolCallResult.Mode.NEW);
        assertThat(call.query().filters().getFirst().op()).isEqualTo(IqlQuery.Operator.EQ);
        assertThat(call.query().orderBy().metricIndex()).isZero();
        assertThat(call.query().groupBy().getFirst().field()).isEqualTo("source");
    }

    @Test
    void parsesClarificationWithoutCreatingQuery() {
        ToolCallResult result = parser.parse("""
                {"name":"ask_clarification","arguments":{"reason":"ambiguous_reference",
                "question":"Which bucket?","candidates":["first","second"]}}
                """);
        assertThat(result).isInstanceOf(ToolCallResult.AskClarification.class);
    }

    @Test
    void rejectsLegacyNestedQueryInsteadOfSilentlyUsingDefaults() {
        ToolCallResponseParser parser = new ToolCallResponseParser(new ObjectMapper());
        assertThatThrownBy(() -> parser.parse("""
                {"name":"search_events","arguments":{"mode":"new","query":{"filters":[]}}}
                """))
                .isInstanceOf(vdt.se.demo.domain.exception.LlmException.class)
                .hasMessageContaining("arguments.query");
    }
}
