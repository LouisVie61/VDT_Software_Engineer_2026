package vdt.se.demo.application.port.outboundPort;

import vdt.se.demo.domain.model.QueryHistory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QueryHistoryPort {
    void save(QueryHistory queryHistory);

    List<QueryHistory> findRecent(String userIdentity, int limit);

    Optional<QueryHistory> findById(UUID id);
}
