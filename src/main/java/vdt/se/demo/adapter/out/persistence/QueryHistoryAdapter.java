package vdt.se.demo.adapter.out.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import vdt.se.demo.application.port.outboundPort.history.QueryHistoryPort;
import vdt.se.demo.domain.model.QueryHistory;
import vdt.se.demo.domain.valueObjects.ChartType;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class QueryHistoryAdapter implements QueryHistoryPort {
    private static final Logger log = LoggerFactory.getLogger(QueryHistoryAdapter.class);

    private final JdbcTemplate jdbcTemplate;

    public QueryHistoryAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void save(QueryHistory queryHistory) {
        log.debug("Query history DB insert started: queryId={}, userIdentity={}, totalCount={}",
                queryHistory.id(), queryHistory.userIdentity(), queryHistory.totalCount());
        jdbcTemplate.update("""
                        INSERT INTO query_history (
                            id, user_identity, session_id, nl_query, generated_dsl, summary,
                            chart_type, total_count, result_snapshot, created_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                queryHistory.id(),
                queryHistory.userIdentity(),
                queryHistory.sessionId(),
                queryHistory.nlQuery(),
                queryHistory.generatedDsl(),
                queryHistory.summary(),
                queryHistory.chartType().name(),
                queryHistory.totalCount(),
                queryHistory.resultSnapshot(),
                queryHistory.createdAt() == null ? LocalDateTime.now() : queryHistory.createdAt()
        );
        log.debug("Query history DB insert completed: queryId={}", queryHistory.id());
    }

    @Override
    public List<QueryHistory> findRecent(String userIdentity, int limit) {
        int boundedLimit = Math.max(1, Math.min(limit, 100));
        log.debug("Query history DB find recent started: userIdentity={}, limit={}", userIdentity, boundedLimit);
        List<QueryHistory> rows = jdbcTemplate.query("""
                        SELECT * FROM query_history
                        WHERE user_identity = ?
                        ORDER BY created_at DESC
                        LIMIT ?
                        """,
                this::mapRow,
                userIdentity,
                boundedLimit
        );
        log.debug("Query history DB find recent completed: userIdentity={}, rows={}", userIdentity, rows.size());
        return rows;
    }

    @Override
    public List<QueryHistory> findRecent(String userIdentity, String sessionId, int limit) {
        int boundedLimit = Math.max(1, Math.min(limit, 100));
        log.debug("Query history DB find recent by session started: userIdentity={}, sessionId={}, limit={}",
                userIdentity, sessionId, boundedLimit);
        List<QueryHistory> rows = jdbcTemplate.query("""
                        SELECT * FROM query_history
                        WHERE user_identity = ? AND session_id = ?
                        ORDER BY created_at DESC
                        LIMIT ?
                        """,
                this::mapRow,
                userIdentity,
                sessionId,
                boundedLimit
        );
        log.debug("Query history DB find recent by session completed: userIdentity={}, sessionId={}, rows={}",
                userIdentity, sessionId, rows.size());
        return rows;
    }

    @Override
    public Optional<QueryHistory> findById(UUID id) {
        log.debug("Query history DB find by id started: queryId={}", id);
        List<QueryHistory> rows = jdbcTemplate.query(
                "SELECT * FROM query_history WHERE id = ?",
                this::mapRow,
                id
        );
        log.debug("Query history DB find by id completed: queryId={}, found={}", id, !rows.isEmpty());
        return rows.stream().findFirst();
    }

    private QueryHistory mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new QueryHistory(
                rs.getObject("id", UUID.class),
                rs.getString("user_identity"),
                rs.getString("session_id"),
                rs.getString("nl_query"),
                rs.getString("generated_dsl"),
                rs.getString("summary"),
                ChartType.fromString(rs.getString("chart_type")),
                rs.getInt("total_count"),
                rs.getString("result_snapshot"),
                rs.getObject("created_at", LocalDateTime.class)
        );
    }
}
