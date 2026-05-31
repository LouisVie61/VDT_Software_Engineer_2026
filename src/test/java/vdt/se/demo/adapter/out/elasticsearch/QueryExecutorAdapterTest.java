package vdt.se.demo.adapter.out.elasticsearch;

import org.junit.jupiter.api.Test;
import org.springframework.data.elasticsearch.core.AggregationsContainer;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.domain.model.ExecutionResult;
import vdt.se.demo.domain.service.EventDocumentMapper;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QueryExecutorAdapterTest {

    @Test
    void executesRawDslAndMapsHits() {
        ElasticsearchOperations operations = mock(ElasticsearchOperations.class);
        SearchHits<SocEventDocument> hits = mock(SearchHits.class);
        SearchHit<SocEventDocument> hit = mock(SearchHit.class);
        SocEventDocument document = new SocEventDocument();
        document.setUser("alice");
        document.setIp("10.0.0.1");

        when(hit.getContent()).thenReturn(document);
        when(hits.stream()).thenReturn(Stream.of(hit));
        when(hits.getTotalHits()).thenReturn(1L);
        when(hits.getAggregations()).thenReturn(null);
        when(operations.search(any(StringQuery.class), eq(SocEventDocument.class))).thenReturn(hits);

        QueryExecutorAdapter adapter = new QueryExecutorAdapter(operations, new ObjectMapper(), new EventDocumentMapper());
        JsonNode dsl = new ObjectMapper().readTree("{\"query\":{\"match_all\":{}},\"size\":10}");

        ExecutionResult result = adapter.execute(dsl);

        assertThat(result.totalCount()).isEqualTo(1);
        assertThat(result.results()).singleElement().satisfies(row -> {
            assertThat(row).containsEntry("user", "alice");
            assertThat(row).containsEntry("ip", "10.0.0.1");
        });
        assertThat(result.aggregations()).isEmpty();
    }

    @Test
    void readsAggregationBucketsFromSearchHits() {
        ElasticsearchOperations operations = mock(ElasticsearchOperations.class);
        SearchHits<SocEventDocument> hits = mock(SearchHits.class);
        AggregationsContainer<List<FakeAggregationHolder>> aggregations = () -> List.of(
                new FakeAggregationHolder(new FakeAggregation("top_values", new FakeAggregate()))
        );

        when(hits.stream()).thenReturn(Stream.empty());
        when(hits.getTotalHits()).thenReturn(0L);
        doReturn(aggregations).when(hits).getAggregations();
        when(operations.search(any(StringQuery.class), eq(SocEventDocument.class))).thenReturn(hits);

        QueryExecutorAdapter adapter = new QueryExecutorAdapter(operations, new ObjectMapper(), new EventDocumentMapper());
        JsonNode dsl = new ObjectMapper().readTree("""
                {"query":{"match_all":{}},"size":0,"aggs":{"top_values":{"terms":{"field":"ip","size":10}}}}
                """);

        ExecutionResult result = adapter.execute(dsl);

        assertThat(result.aggregations()).containsExactly(Map.of(
                "aggregation", "top_values",
                "key", "10.0.0.1",
                "count", 7L
        ));
    }

    private record FakeAggregationHolder(FakeAggregation aggregation) {
    }

    private record FakeAggregation(String getName, FakeAggregate getAggregate) {
    }

    private static class FakeAggregate {
        public FakeTermsAggregate sterms() {
            return new FakeTermsAggregate();
        }
    }

    private static class FakeTermsAggregate {
        public FakeBuckets buckets() {
            return new FakeBuckets();
        }
    }

    private static class FakeBuckets {
        public List<FakeBucket> array() {
            return List.of(new FakeBucket());
        }
    }

    private static class FakeBucket {
        public String key() {
            return "10.0.0.1";
        }

        public long docCount() {
            return 7L;
        }
    }
}
