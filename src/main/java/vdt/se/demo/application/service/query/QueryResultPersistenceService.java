package vdt.se.demo.application.service.query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.adapter.config.AppProperties;
import vdt.se.demo.application.dto.SearchRequest;
import vdt.se.demo.application.port.outboundPort.history.QueryHistoryPort;
import vdt.se.demo.domain.model.QueryHistory;
import vdt.se.demo.domain.model.QueryResult;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.UUID;

public class QueryResultPersistenceService {
    private static final Logger log = LoggerFactory.getLogger(QueryResultPersistenceService.class);

    private final QueryHistoryPort queryHistoryPort;
    private final ObjectMapper objectMapper;
    private final AppProperties properties;

    public QueryResultPersistenceService(QueryHistoryPort queryHistoryPort, ObjectMapper objectMapper,
                                         AppProperties properties) {
        this.queryHistoryPort = queryHistoryPort;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public void save(UUID queryId, SearchRequest request, QueryResult result, String generatedDsl) throws Exception {
        log.debug("Persisting query result: queryId={}, chartType={}, totalCount={}, resultRows={}",
                queryId, result.getChartType(), result.getTotalCount(),
                rowCount(result.getResults()));
        queryHistoryPort.save(new QueryHistory(
                queryId,
                properties.getUser().getDefaultId(),
                request.getSessionId(),
                request.getQuestion(),
                generatedDsl,
                result.getSummary(),
                result.getChartType(),
                result.getTotalCount(),
                objectMapper.writeValueAsString(result.getResults()),
                LocalDateTime.now()
        ));
        log.debug("Query result persisted: queryId={}", queryId);
    }

    private int rowCount(Object rows) {
        return rows instanceof Collection<?> collection ? collection.size() : 0;
    }
}
