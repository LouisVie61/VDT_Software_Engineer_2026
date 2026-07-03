package vdt.se.demo.application.service.cache;

import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.domain.iql.IqlQuery;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

public final class IqlCacheKeyService {
    private final ObjectMapper mapper;
    public IqlCacheKeyService(ObjectMapper mapper) { this.mapper = mapper; }
    public String key(IqlQuery query) {
        IqlQuery cursorless = new IqlQuery(query.select(), query.filters(), query.filterLogic(), query.timeRange(),
                query.groupBy(), query.metrics(), query.orderBy(), query.sort(), query.size(), null);
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(mapper.writeValueAsBytes(cursorless))); }
        catch (Exception e) { throw new IllegalStateException("Cannot hash IQL", e); }
    }
}
