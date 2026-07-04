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

class IqlQueryNormalizerTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final IqlQueryNormalizer normalizer = new IqlQueryNormalizer(mapper,
            Clock.fixed(Instant.parse("2026-07-04T00:00:00Z"), ZoneOffset.UTC));

    @Test
    void appliesStructuredOverridesAndDefaultAbsoluteTimeRange() {
        IqlQuery input = query(List.of(new IqlQuery.FilterCondition("llm-severity", "severity", IqlQuery.Operator.EQ,
                mapper.valueToTree("low"))), null);

        IqlQuery result = normalizer.normalize(input,
                new SearchConstraints(null, null, "critical", null, null, "web-1", null));

        assertThat(result.filters()).extracting(IqlQuery.FilterCondition::id)
                .containsExactly("request-severity", "request-host");
        assertThat(result.timeRange().from()).isEqualTo("2026-07-03T00:00:00Z");
        assertThat(result.timeRange().to()).isEqualTo("2026-07-04T00:00:00Z");
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

    private IqlQuery query(List<IqlQuery.FilterCondition> filters, IqlQuery.TimeRange range) {
        return new IqlQuery(List.of(), filters, null, range, List.of(), List.of(), null, List.of(), 50, null);
    }
}
