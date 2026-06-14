package vdt.se.demo.adapter.out.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import vdt.se.demo.application.port.outboundPort.QueryHistoryPort;
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

    private final JdbcTemplate jdbcTemplate;

    public QueryHistoryAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void save(QueryHistory queryHistory) {
        jdbcTemplate.update("""
                        INSERT INTO query_history (
                            id, user_identity, nl_query, generated_dsl, summary,
                            chart_type, total_count, result_snapshot, created_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                queryHistory.id(),
                queryHistory.userIdentity(),
                queryHistory.nlQuery(),
                queryHistory.generatedDsl(),
                queryHistory.summary(),
                queryHistory.chartType().name(),
                queryHistory.totalCount(),
                queryHistory.resultSnapshot(),
                queryHistory.createdAt() == null ? LocalDateTime.now() : queryHistory.createdAt()
        );
    }

    @Override
    public List<QueryHistory> findRecent(String userIdentity, int limit) {
        return jdbcTemplate.query("""
                        SELECT * FROM query_history
                        WHERE user_identity = ?
                        ORDER BY created_at DESC
                        LIMIT ?
                        """,
                this::mapRow,
                userIdentity,
                Math.max(1, Math.min(limit, 100))
        );
    }

    @Override
    public Optional<QueryHistory> findById(UUID id) {
        List<QueryHistory> rows = jdbcTemplate.query(
                "SELECT * FROM query_history WHERE id = ?",
                this::mapRow,
                id
        );
        return rows.stream().findFirst();
    }

    private QueryHistory mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new QueryHistory(
                rs.getObject("id", UUID.class),
                rs.getString("user_identity"),
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
