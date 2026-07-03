package vdt.se.demo.adapter.out.llm;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.domain.iql.IqlQuery;
import vdt.se.demo.domain.iql.ToolCallResult;

import static org.assertj.core.api.Assertions.assertThat;

class ToolCallResponseParserTest {
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
}
