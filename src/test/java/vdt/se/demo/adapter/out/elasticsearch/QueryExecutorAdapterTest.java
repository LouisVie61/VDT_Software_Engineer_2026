package vdt.se.demo.adapter.out.elasticsearch;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.domain.model.ExecutionResult;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QueryExecutorAdapterTest {

    @Test
    void delegatesRawDslExecutionAndResponseMapping() throws Exception {
        ElasticsearchHttpClient client = mock(ElasticsearchHttpClient.class);
        ElasticsearchSearchResponseMapper mapper = mock(ElasticsearchSearchResponseMapper.class);
        JsonNode dsl = new ObjectMapper().readTree("{\"query\":{\"match_all\":{}}}");
        JsonNode response = new ObjectMapper().readTree("{\"hits\":{\"hits\":[]}}");
        ExecutionResult expected = new ExecutionResult(List.of(Map.of("user", "alice")), List.of(), 1);
        when(client.search(dsl)).thenReturn(response);
        when(mapper.map(response)).thenReturn(expected);

        ExecutionResult result = new QueryExecutorAdapter(client, mapper).execute(dsl);

        assertThat(result).isEqualTo(expected);
    }
}
