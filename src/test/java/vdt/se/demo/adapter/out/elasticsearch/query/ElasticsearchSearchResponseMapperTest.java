package vdt.se.demo.adapter.out.elasticsearch.query;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.domain.model.ExecutionResult;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ElasticsearchSearchResponseMapperTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ElasticsearchSearchResponseMapper mapper = new ElasticsearchSearchResponseMapper();

    @Test
    void mapsHitsAndAggregationBuckets() {
        ExecutionResult result = mapper.map(objectMapper.readTree("""
                {
                  "hits": {
                    "total": {"value": 1},
                    "hits": [{"_id": "1", "_source": {"user": "alice", "ip": "10.0.0.1"}}]
                  },
                  "aggregations": {
                    "top_values": {"buckets": [{"key": "10.0.0.1", "doc_count": 7}]}
                  }
                }
                """));

        assertThat(result.totalCount()).isEqualTo(1);
        assertThat(result.results()).singleElement().satisfies(row -> assertThat(row).containsEntry("user", "alice"));
        assertThat(result.aggregations()).containsExactly(Map.of(
                "aggregation", "top_values", "key", "10.0.0.1", "count", 7
        ));
    }

    @Test
    void mapsMissingHitsAndAggregationsToEmptyResult() {
        ExecutionResult result = mapper.map(objectMapper.readTree("{}"));

        assertThat(result.totalCount()).isZero();
        assertThat(result.results()).isEmpty();
        assertThat(result.aggregations()).isEmpty();
    }

    @Test
    void mapsMetricAggregationValues() {
        ExecutionResult result = mapper.map(objectMapper.readTree("""
                {
                  "hits": {"total": {"value": 0}, "hits": []},
                  "aggregations": {
                    "latest_timestamp": {
                      "value": 1908566105000,
                      "value_as_string": "2030-06-24T21:15:05.000Z"
                    }
                  }
                }
                """));

        assertThat(result.aggregations()).containsExactly(Map.of(
                "aggregation", "latest_timestamp",
                "value", 1.908566105E12,
                "value_as_string", "2030-06-24T21:15:05.000Z"
        ));
    }

    @Test
    void mapsTopLevelFilterAggregationCounts() {
        ExecutionResult result = mapper.map(objectMapper.readTree("""
                {
                  "hits": {"total": {"value": 10}, "hits": []},
                  "aggregations": {
                    "critical_events": {"doc_count": 4}
                  }
                }
                """));

        assertThat(result.aggregations()).containsExactly(Map.of(
                "aggregation", "critical_events",
                "count", 4
        ));
    }

    @Test
    @SuppressWarnings("unchecked")
    void mapsNestedAggregationBucketsIntoParentRows() {
        ExecutionResult result = mapper.map(objectMapper.readTree("""
                {
                  "hits": {"total": {"value": 42}, "hits": []},
                  "aggregations": {
                    "events": {
                      "buckets": [{
                        "key_as_string": "2025-07-14T00:00:00.000Z",
                        "key": 1752451200000,
                        "doc_count": 12,
                        "event_type": {
                          "buckets": [
                            {"key": "auth", "doc_count": 7},
                            {"key": "network", "doc_count": 3}
                          ]
                        }
                      }]
                    }
                  }
                }
                """));

        assertThat(result.aggregations()).hasSize(1);
        Map<String, Object> row = result.aggregations().getFirst();
        assertThat(row).containsEntry("key", "2025-07-14T00:00:00.000Z")
                .containsEntry("count", 12);
        assertThat((List<Map<String, Object>>) row.get("event_type_buckets"))
                .containsExactly(
                        Map.of("key", "auth", "count", 7),
                        Map.of("key", "network", "count", 3)
                );
    }
}
