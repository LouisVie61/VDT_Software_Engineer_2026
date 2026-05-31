package vdt.se.demo.application.port.inboundPort;

import vdt.se.demo.application.dto.SearchRequest;
import vdt.se.demo.domain.model.QueryHistory;
import vdt.se.demo.domain.model.QueryResult;

import java.util.List;
import java.util.UUID;

public interface QueryUseCase {
    QueryResult search(SearchRequest request);

    List<QueryHistory> history(String userIdentity, int limit);

    String exportCsv(UUID queryId);
}
