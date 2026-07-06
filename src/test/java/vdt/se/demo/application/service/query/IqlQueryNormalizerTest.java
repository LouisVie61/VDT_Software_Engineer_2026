package vdt.se.demo.application.service.query;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.domain.iql.IqlQuery;
import vdt.se.demo.domain.iql.SearchConstraints;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IqlQueryNormalizerTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final IqlQueryNormalizer normalizer = new IqlQueryNormalizer(mapper,
            Clock.fixed(Instant.parse("2026-07-04T00:00:00Z"), ZoneOffset.UTC));

    @Test
    void appliesStructuredOverridesWithoutInventingTimeRange() {
        IqlQuery input = query(List.of(new IqlQuery.FilterCondition("llm-severity", "severity", IqlQuery.Operator.EQ,
                mapper.valueToTree("low"))), null);

        IqlQuery result = normalizer.normalize(input,
                new SearchConstraints(null, null, "critical", null, null, "web-1", null));

        assertThat(result.filters()).extracting(IqlQuery.FilterCondition::id)
                .containsExactly("request-severity", "request-host");
        assertThat(result.timeRange()).isNull();
    }

    @Test
    void migratesTimestampFiltersAndResolvesRelativeValues() {
        IqlQuery input = query(List.of(
                new IqlQuery.FilterCondition("from", "timestamp", IqlQuery.Operator.GTE, mapper.valueToTree("now-2h")),
                new IqlQuery.FilterCondition("to", "timestamp", IqlQuery.Operator.LT, mapper.valueToTree("now"))), null);

        IqlQuery result = normalizer.normalize(input, SearchConstraints.empty());

        assertThat(result.filters()).isEmpty();
        assertThat(result.timeRange().from()).isEqualTo("2026-07-03T22:00:00Z");
        assertThat(result.timeRange().to()).isEqualTo("2026-07-04T00:00:00Z");
    }

    @Test
    void preservesOneSidedTimeRangeWithoutInventingOtherBound() {
        IqlQuery input = query(List.of(), new IqlQuery.TimeRange("timestamp", "now-2h", null));

        IqlQuery result = normalizer.normalize(input, SearchConstraints.empty());

        assertThat(result.timeRange().from()).isEqualTo("2026-07-03T22:00:00Z");
        assertThat(result.timeRange().to()).isNull();
    }

    @Test
    void preservesElasticsearchDateMathInTimeWindows() {
        IqlQuery input = new IqlQuery(List.of(), List.of(), null,
                new IqlQuery.TimeRange("timestamp", "now/d-1d", "now/d+1d"),
                List.of(), List.of(), null, List.of(), 50, null,
                List.of(new IqlQuery.Window("yesterday",
                        new IqlQuery.TimeRange("timestamp", "now-1d/d", "now/d"), List.of())),
                List.of(), List.of());

        IqlQuery result = normalizer.normalize(input, SearchConstraints.empty());

        assertThat(result.timeRange().from()).isEqualTo("now/d-1d");
        assertThat(result.timeRange().to()).isEqualTo("now/d+1d");
        assertThat(result.windows().getFirst().timeRange().from()).isEqualTo("now-1d/d");
        assertThat(result.windows().getFirst().timeRange().to()).isEqualTo("now/d");
    }

    @Test
    void convertsIsoDateToTheMappedNumericTemporalComponent() {
        IqlQuery input = query(List.of(
                new IqlQuery.FilterCondition("exclude-day", "timestamp_day", IqlQuery.Operator.NEQ,
                        mapper.valueToTree("2025-07-10"))), null);

        IqlQuery result = normalizer.normalize(input, SearchConstraints.empty());

        assertThat(result.filters().getFirst().value().isIntegralNumber()).isTrue();
        assertThat(result.filters().getFirst().value().asInt()).isEqualTo(10);
    }

    @Test
    void normalizesTemporalComponentArraysAndWindowFilters() {
        IqlQuery input = new IqlQuery(List.of(), List.of(), null, null,
                List.of(), List.of(), null, List.of(), 50, null,
                List.of(new IqlQuery.Window("sample",
                        new IqlQuery.TimeRange("timestamp", "2025-07-01T00:00:00Z", "2025-08-01T00:00:00Z"),
                        List.of(new IqlQuery.FilterCondition("months", "timestamp_month", IqlQuery.Operator.IN,
                                mapper.valueToTree(List.of("2025-07-10", "8")))))), List.of(), List.of());

        IqlQuery result = normalizer.normalize(input, SearchConstraints.empty());

        assertThat(result.windows().getFirst().filters().getFirst().value().get(0).asInt()).isEqualTo(7);
        assertThat(result.windows().getFirst().filters().getFirst().value().get(1).asInt()).isEqualTo(8);
    }

    @Test
    void rejectsOutOfRangeTemporalComponentsBeforeDslCompilation() {
        IqlQuery input = query(List.of(
                new IqlQuery.FilterCondition("bad-day", "timestamp_day", IqlQuery.Operator.EQ,
                        mapper.valueToTree("2025-07-99"))), null);

        assertThatThrownBy(() -> normalizer.normalize(input, SearchConstraints.empty()))
                .hasMessageContaining("Invalid numeric value for timestamp_day");
    }

    @Test
    void convertsInclusiveEndOfDayToExclusiveNextDayBoundary() {
        IqlQuery input = query(List.of(),
                new IqlQuery.TimeRange("timestamp", "2025-07-01T00:00:00Z", "2025-07-31T23:59:59Z"));

        IqlQuery result = normalizer.normalize(input, SearchConstraints.empty());

        assertThat(result.timeRange().to()).isEqualTo("2025-08-01T00:00:00Z");
    }

    @Test
    void expandsCompositeAuthenticationFailureAliasIntoCanonicalDimensions() {
        IqlQuery input = query(List.of(new IqlQuery.FilterCondition("auth-failed", "event_type",
                IqlQuery.Operator.EQ, mapper.valueToTree("auth_failed"))), null);

        IqlQuery result = normalizer.normalize(input, SearchConstraints.empty());

        assertThat(result.filters()).extracting(IqlQuery.FilterCondition::field).containsExactly("event_type", "action");
        assertThat(result.filters()).extracting(filter -> filter.value().asString()).containsExactly("auth", "failed");
    }

    @Test
    void rejectsCompositeAliasThatConflictsWithExplicitAction() {
        IqlQuery input = query(List.of(
                new IqlQuery.FilterCondition("auth-failed", "event_type", IqlQuery.Operator.EQ,
                        mapper.valueToTree("auth_failed")),
                new IqlQuery.FilterCondition("allowed", "action", IqlQuery.Operator.EQ,
                        mapper.valueToTree("allowed"))), null);

        assertThatThrownBy(() -> normalizer.normalize(input, SearchConstraints.empty()))
                .hasMessageContaining("conflicts with action filter");
    }

    private IqlQuery query(List<IqlQuery.FilterCondition> filters, IqlQuery.TimeRange range) {
        return new IqlQuery(List.of(), filters, null, range, List.of(), List.of(), null, List.of(), 50, null);
    }
}
