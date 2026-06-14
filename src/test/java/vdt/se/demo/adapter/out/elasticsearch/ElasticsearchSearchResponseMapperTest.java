package vdt.se.demo.adapter.out.elasticsearch;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.domain.model.ExecutionResult;

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
}
