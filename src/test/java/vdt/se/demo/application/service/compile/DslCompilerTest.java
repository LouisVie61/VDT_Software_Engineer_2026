package vdt.se.demo.application.service.compile;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import vdt.se.demo.domain.iql.IqlQuery;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DslCompilerTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final DslCompiler compiler = new DslCompiler(mapper);

    @Test
    void compilesFiltersIntoBoolFilterWithoutScoring() {
        IqlQuery query = new IqlQuery(List.of("timestamp", "severity"), List.of(
                new IqlQuery.FilterCondition("f1", "severity", IqlQuery.Operator.EQ, mapper.valueToTree("critical"))),
                null, new IqlQuery.TimeRange("timestamp", "now-24h", "now"), List.of(), List.of(), null,
                List.of(new IqlQuery.Sort("timestamp", IqlQuery.Direction.DESC)), 25, null);

        ObjectNode dsl = compiler.compile(query);

        assertThat(dsl.at("/query/bool/filter/0/term/severity").asString()).isEqualTo("critical");
        assertThat(dsl.at("/query/bool/filter/1/range/timestamp/gte").asString()).isEqualTo("now-24h");
        assertThat(dsl.at("/query/bool/filter/1/range/timestamp/lt").asString()).isEqualTo("now");
        assertThat(dsl.at("/query/bool/must").isMissingNode()).isTrue();
        assertThat(dsl.path("track_total_hits").asBoolean()).isTrue();
        assertThat(dsl.at("/sort/1/event_id/order").asString()).isEqualTo("asc");
    }

    @Test
    void selectsCompositeAndBucketSortForMultiGroupMetricOrdering() {
        IqlQuery query = new IqlQuery(List.of(), List.of(), null, null,
                List.of(new IqlQuery.GroupBy("source", 20), new IqlQuery.GroupBy("severity", 20)),
                List.of(new IqlQuery.Metric(IqlQuery.MetricType.CARDINALITY, "host")),
                new IqlQuery.OrderBy(IqlQuery.OrderTarget.METRIC, 0, IqlQuery.Direction.DESC), List.of(), 50, null);

        ObjectNode dsl = compiler.compile(query);

        assertThat(dsl.at("/aggs/events/composite/sources").size()).isEqualTo(2);
        assertThat(dsl.at("/aggs/events/aggs/ordered_buckets/bucket_sort").isObject()).isTrue();
    }

    @Test
    void compilesPerBucketSampleHitsPreview() {
        IqlQuery.SampleHits samples = new IqlQuery.SampleHits(null,
                List.of(new IqlQuery.Sort("timestamp", IqlQuery.Direction.DESC)));
        IqlQuery query = new IqlQuery(List.of(), List.of(), null, null,
                List.of(new IqlQuery.GroupBy("host", 5, samples)),
                List.of(new IqlQuery.Metric(IqlQuery.MetricType.COUNT, null)), null, List.of(), 50, null);

        ObjectNode dsl = compiler.compile(query);

        assertThat(dsl.at("/aggs/events/aggs/sample_hits/top_hits/size").asInt()).isEqualTo(5);
        assertThat(dsl.at("/aggs/events/aggs/sample_hits/top_hits/sort/0/timestamp/order").asString())
                .isEqualTo("desc");
    }
}
