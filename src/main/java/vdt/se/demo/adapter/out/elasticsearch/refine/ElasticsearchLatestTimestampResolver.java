package vdt.se.demo.adapter.out.elasticsearch.refine;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import vdt.se.demo.application.port.outboundPort.execution.QueryExecutorPort;
import vdt.se.demo.domain.model.ExecutionResult;

import java.time.Instant;
import java.util.Optional;

@Component
public class ElasticsearchLatestTimestampResolver {

    private final QueryExecutorPort queryExecutorPort;
    private final ObjectMapper objectMapper;

    public ElasticsearchLatestTimestampResolver(QueryExecutorPort queryExecutorPort, ObjectMapper objectMapper) {
        this.queryExecutorPort = queryExecutorPort;
        this.objectMapper = objectMapper;
    }

    public Optional<Instant> resolve(JsonNode dsl) throws Exception {
        ExecutionResult result = queryExecutorPort.execute(latestTimestampDsl(dsl));
        return result.aggregations().stream()
                .filter(row -> "latest_timestamp".equals(row.get("aggregation")))
                .map(row -> row.get("value_as_string"))
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .findFirst()
                .map(Instant::parse);
    }

    private JsonNode latestTimestampDsl(JsonNode dsl) throws Exception {
        ObjectNode maxDsl = objectMapper.createObjectNode();
        JsonNode query = dsl == null ? null : dsl.get("query");
        maxDsl.set("query", query == null || query.isNull() ? objectMapper.readTree("{\"match_all\":{}}") : query);
        maxDsl.put("size", 0);
        ObjectNode latestTimestamp = maxDsl.putObject("aggs").putObject("latest_timestamp");
        latestTimestamp.putObject("max").put("field", "timestamp");
        return maxDsl;
    }
}
