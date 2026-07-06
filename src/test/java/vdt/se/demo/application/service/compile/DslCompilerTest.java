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
    void compilesCountRankedMultiGroupAsNestedBuckets() {
        IqlQuery query = new IqlQuery(List.of(),
                List.of(new IqlQuery.FilterCondition("severity_critical", "severity", IqlQuery.Operator.EQ,
                        mapper.valueToTree("critical"))),
                null, new IqlQuery.TimeRange("timestamp", "2025-07-01T00:00:00Z", "2025-08-01T00:00:00Z"),
                List.of(new IqlQuery.GroupBy("timestamp_day", 1), new IqlQuery.GroupBy("event_type", 3)),
                List.of(new IqlQuery.Metric(IqlQuery.MetricType.COUNT, null)),
                new IqlQuery.OrderBy(IqlQuery.OrderTarget.COUNT, null, IqlQuery.Direction.DESC), List.of(), 50, null);

        ObjectNode dsl = compiler.compile(query);

        assertThat(dsl.at("/aggs/events/date_histogram/field").asString()).isEqualTo("timestamp");
        assertThat(dsl.at("/aggs/events/date_histogram/calendar_interval").asString()).isEqualTo("day");
        assertThat(dsl.at("/aggs/events/aggs/limit_timestamp_day/bucket_sort/size").asInt()).isEqualTo(1);
        assertThat(dsl.at("/aggs/events/aggs/event_type/terms/field").asString()).isEqualTo("event_type");
        assertThat(dsl.at("/aggs/events/aggs/event_type/terms/size").asInt()).isEqualTo(3);
        assertThat(dsl.at("/aggs/events/composite").isMissingNode()).isTrue();
    }

    @Test
    void keepsUnrankedMultiGroupAsComposite() {
        IqlQuery query = new IqlQuery(List.of(), List.of(), null, null,
                List.of(new IqlQuery.GroupBy("source", 20), new IqlQuery.GroupBy("severity", 20)),
                List.of(new IqlQuery.Metric(IqlQuery.MetricType.COUNT, null)),
                null, List.of(), 50, null);

        ObjectNode dsl = compiler.compile(query);

        assertThat(dsl.at("/aggs/events/composite/sources").size()).isEqualTo(2);
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

    @Test
    void compilesTemporalDimensionsAsDateHistograms() {
        IqlQuery query = new IqlQuery(List.of(), List.of(), null,
                new IqlQuery.TimeRange("timestamp", "2023-01-01T00:00:00Z", "2024-01-01T00:00:00Z"),
                List.of(new IqlQuery.GroupBy("timestamp_quarter", 10)),
                List.of(new IqlQuery.Metric(IqlQuery.MetricType.COUNT, null)), null, List.of(), 50, null);

        ObjectNode dsl = compiler.compile(query);

        assertThat(dsl.at("/aggs/events/date_histogram/field").asString()).isEqualTo("timestamp");
        assertThat(dsl.at("/aggs/events/date_histogram/calendar_interval").asString()).isEqualTo("quarter");
        assertThat(dsl.at("/aggs/events/terms").isMissingNode()).isTrue();
    }

    @Test
    void compilesWindowedSetDifferenceAsBucketSelector() {
        IqlQuery query = new IqlQuery(List.of(), List.of(), null,
                new IqlQuery.TimeRange("timestamp", "2026-07-03T00:00:00Z", "2026-07-05T00:00:00Z"),
                List.of(new IqlQuery.GroupBy("severity", 1000)),
                List.of(new IqlQuery.Metric(IqlQuery.MetricType.COUNT, null)), null, List.of(), 50, null,
                List.of(
                        new IqlQuery.Window("today", new IqlQuery.TimeRange("timestamp", "2026-07-04T00:00:00Z", "2026-07-05T00:00:00Z"), List.of()),
                        new IqlQuery.Window("yesterday", new IqlQuery.TimeRange("timestamp", "2026-07-03T00:00:00Z", "2026-07-04T00:00:00Z"), List.of())),
                List.of(
                        new IqlQuery.HavingCondition("count", "today", IqlQuery.ComparisonOp.GT, 0.0),
                        new IqlQuery.HavingCondition("count", "yesterday", IqlQuery.ComparisonOp.EQ, 0.0)),
                List.of());

        ObjectNode dsl = compiler.compile(query);

        assertThat(dsl.at("/aggs/events/terms/field").asString()).isEqualTo("severity");
        assertThat(dsl.at("/aggs/events/terms/size").asInt()).isEqualTo(1000);
        assertThat(dsl.at("/aggs/events/aggs/today/filter/range/timestamp/gte").asString()).isEqualTo("2026-07-04T00:00:00Z");
        assertThat(dsl.at("/aggs/events/aggs/having/bucket_selector/buckets_path/p0").asString()).isEqualTo("today>_count");
        assertThat(dsl.at("/aggs/events/aggs/having/bucket_selector/script/source").asString())
                .isEqualTo("params.p0 > 0.0 && params.p1 == 0.0");
    }

    @Test
    void compilesHavingThresholdAgainstBucketCount() {
        IqlQuery query = new IqlQuery(List.of(), List.of(), null, null,
                List.of(new IqlQuery.GroupBy("source", 1000)),
                List.of(new IqlQuery.Metric(IqlQuery.MetricType.COUNT, null)), null, List.of(), 50, null,
                List.of(), List.of(new IqlQuery.HavingCondition("count", null, IqlQuery.ComparisonOp.GT, 100.0)),
                List.of());

        ObjectNode dsl = compiler.compile(query);

        assertThat(dsl.at("/aggs/events/aggs/having/bucket_selector/buckets_path/p0").asString()).isEqualTo("_count");
        assertThat(dsl.at("/aggs/events/aggs/having/bucket_selector/script/source").asString()).isEqualTo("params.p0 > 100.0");
    }

    @Test
    void compilesPercentageDerivedMetricAsBucketScript() {
        IqlQuery.Window critical = new IqlQuery.Window("critical_events",
                new IqlQuery.TimeRange("timestamp", "now-24h", "now"),
                List.of(new IqlQuery.FilterCondition("critical", "severity", IqlQuery.Operator.EQ, mapper.valueToTree("critical"))));
        IqlQuery query = new IqlQuery(List.of(), List.of(), null,
                new IqlQuery.TimeRange("timestamp", "now-24h", "now"),
                List.of(new IqlQuery.GroupBy("source", 1000)),
                List.of(new IqlQuery.Metric(IqlQuery.MetricType.COUNT, null)), null, List.of(), 50, null,
                List.of(critical),
                List.of(),
                List.of(new IqlQuery.DerivedMetric("critical_percent", IqlQuery.DerivedMetricType.PERCENT,
                        new IqlQuery.MetricRef("count", "critical_events"), new IqlQuery.MetricRef("count", null))));

        ObjectNode dsl = compiler.compile(query);

        assertThat(dsl.at("/aggs/events/aggs/critical_percent/bucket_script/buckets_path/num").asString())
                .isEqualTo("critical_events>_count");
        assertThat(dsl.at("/aggs/events/aggs/critical_percent/bucket_script/buckets_path/den").asString())
                .isEqualTo("_count");
        assertThat(dsl.at("/aggs/events/aggs/critical_events/filter/bool/filter/1/term/severity").asString())
                .isEqualTo("critical");
    }

    @Test
    void ordersAndLimitsDateBucketsByDerivedMetric() {
        IqlQuery.Window failed = new IqlQuery.Window("failed_auth",
                new IqlQuery.TimeRange("timestamp", "2025-07-01T00:00:00Z", "2025-08-01T00:00:00Z"),
                List.of(new IqlQuery.FilterCondition("failed", "action", IqlQuery.Operator.EQ,
                        mapper.valueToTree("failed"))));
        IqlQuery query = new IqlQuery(List.of(), List.of(), null,
                new IqlQuery.TimeRange("timestamp", "2025-07-01T00:00:00Z", "2025-08-01T00:00:00Z"),
                List.of(new IqlQuery.GroupBy("timestamp_day", 1)),
                List.of(new IqlQuery.Metric(IqlQuery.MetricType.COUNT, null)),
                new IqlQuery.OrderBy(IqlQuery.OrderTarget.DERIVED_METRIC, 0, IqlQuery.Direction.DESC),
                List.of(), 50, null, List.of(failed),
                List.of(new IqlQuery.HavingCondition("count", null, IqlQuery.ComparisonOp.GTE, 20.0)),
                List.of(new IqlQuery.DerivedMetric("failed_rate", IqlQuery.DerivedMetricType.RATIO,
                        new IqlQuery.MetricRef("count", "failed_auth"), new IqlQuery.MetricRef("count", null))));

        ObjectNode dsl = compiler.compile(query);

        assertThat(dsl.at("/aggs/events/date_histogram/order").isMissingNode()).isTrue();
        assertThat(dsl.at("/aggs/events/aggs/ordered_buckets/bucket_sort/sort/0/failed_rate/order").asString())
                .isEqualTo("desc");
        assertThat(dsl.at("/aggs/events/aggs/ordered_buckets/bucket_sort/size").asInt()).isEqualTo(1);
    }

    @Test
    void enforcesTopCountLimitForSingleDateHistogram() {
        IqlQuery query = new IqlQuery(List.of(), List.of(), null, null,
                List.of(new IqlQuery.GroupBy("timestamp_day", 1)),
                List.of(new IqlQuery.Metric(IqlQuery.MetricType.COUNT, null)),
                new IqlQuery.OrderBy(IqlQuery.OrderTarget.COUNT, null, IqlQuery.Direction.DESC),
                List.of(), 50, null);

        ObjectNode dsl = compiler.compile(query);

        assertThat(dsl.at("/aggs/events/date_histogram/order/_count").asString()).isEqualTo("desc");
        assertThat(dsl.at("/aggs/events/aggs/limit_timestamp_day/bucket_sort/size").asInt()).isEqualTo(1);
    }
}
