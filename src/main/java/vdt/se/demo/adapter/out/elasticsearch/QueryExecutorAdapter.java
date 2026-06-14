package vdt.se.demo.adapter.out.elasticsearch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import vdt.se.demo.application.port.outboundPort.QueryExecutorPort;
import vdt.se.demo.domain.exception.BadQueryException;
import vdt.se.demo.domain.model.ExecutionResult;

@Component
public class QueryExecutorAdapter implements QueryExecutorPort {

    private static final Logger log = LoggerFactory.getLogger(QueryExecutorAdapter.class);

    private final ElasticsearchHttpClient httpClient;
    private final ElasticsearchSearchResponseMapper responseMapper;

    public QueryExecutorAdapter(ElasticsearchHttpClient httpClient, ElasticsearchSearchResponseMapper responseMapper) {
        this.httpClient = httpClient;
        this.responseMapper = responseMapper;
    }

    @Override
    public ExecutionResult execute(JsonNode generatedDsl) {
        try {
            return responseMapper.map(httpClient.search(generatedDsl));
        } catch (BadQueryException e) {
            throw e;
        } catch (Exception e) {
            log.error("Cannot execute Elasticsearch DSL: {}", generatedDsl, e);
            throw new BadQueryException("Cannot execute Elasticsearch DSL", e);
        }
    }
}
