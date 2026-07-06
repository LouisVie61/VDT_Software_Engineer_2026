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

    @Test
    void toolSchemaDefinesWindowHavingAndDerivedMetricContracts() {
        JsonNode schema = new vdt.se.demo.application.service.llm.LlmToolDefinitions(new ObjectMapper())
                .searchEvents().path("input_schema").path("properties");

        assertThat(schema.path("windows").isObject()).isTrue();
        assertThat(schema.path("having").path("items").path("properties").path("metric").path("enum").toString())
                .contains("count");
        assertThat(schema.path("derived_metrics").path("items").path("properties").path("type").path("enum").toString())
                .contains("percent", "ratio");
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
    void parsesGroupSampleHits() {
        ToolCallResult.SearchEvents call = (ToolCallResult.SearchEvents) parser.parse("""
                {"name":"search_events","arguments":{"mode":"new","group_by":[{"field":"host","size":5,
                "sample_hits":{"size":3,"sort":[{"field":"timestamp","order":"desc"}]}}]}}
                """);

        IqlQuery.SampleHits samples = call.query().groupBy().getFirst().sampleHits();
        assertThat(samples.effectiveSize()).isEqualTo(3);
        assertThat(samples.sort().getFirst().field()).isEqualTo("timestamp");
    }

    @Test
    void parsesWindowsHavingAndDerivedMetrics() {
        ToolCallResult.SearchEvents call = (ToolCallResult.SearchEvents) parser.parse("""
                {"name":"search_events","arguments":{"mode":"new",
                "group_by":[{"field":"severity","size":1000}],
                "metrics":[{"type":"count"}],
                "windows":[{"name":"today","time_range":{"field":"timestamp","from":"now-1d","to":"now"}}],
                "having":[{"metric":"count","window":"today","op":"gt","value":0}],
                "derived_metrics":[{"name":"today_percent","type":"percent",
                  "numerator":{"metric":"count","window":"today"},"denominator":{"metric":"count"}}],
                "order_by":{"target":"derived_metric","metric_index":0,"direction":"desc"}}}
                """);

        assertThat(call.query().windows().getFirst().name()).isEqualTo("today");
        assertThat(call.query().having().getFirst().op()).isEqualTo(IqlQuery.ComparisonOp.GT);
        assertThat(call.query().derivedMetrics().getFirst().type()).isEqualTo(IqlQuery.DerivedMetricType.PERCENT);
        assertThat(call.query().orderBy().target()).isEqualTo(IqlQuery.OrderTarget.DERIVED_METRIC);
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

    @Test
    void parsesNativeFilterValuesWithoutJsonEncodedStrings() {
        ToolCallResult.SearchEvents call = (ToolCallResult.SearchEvents) parser.parse("""
                {"name":"search_events","arguments":{"mode":"new","filters":[
                  {"id":"failed","field":"action","op":"eq","values":["failed"]},
                  {"id":"days","field":"timestamp_day","op":"not_in","values":["10","20"]}],
                  "group_by":[{"field":"timestamp_day","size":5}]}}
                """);

        assertThat(call.query().filters().getFirst().value().asString()).isEqualTo("failed");
        assertThat(call.query().filters().get(1).value()).hasSize(2);
        assertThat(call.query().groupBy().getFirst().effectiveSize()).isEqualTo(5);
    }

    @Test
    void rejectsScalarOperatorWithStructuralFragmentsAsMultipleValues() {
        assertThatThrownBy(() -> parser.parse("""
                {"name":"search_events","arguments":{"mode":"new","filters":[
                  {"id":"bad","field":"action","op":"eq","values":["failed","}],group_by"]}]}}
                """))
                .isInstanceOf(vdt.se.demo.domain.exception.LlmException.class)
                .hasMessageContaining("requires exactly one");
    }
}
