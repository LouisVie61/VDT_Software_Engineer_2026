package vdt.se.demo.application.service.cache;

import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.domain.iql.IqlQuery;
import java.security.MessageDigest;
import java.util.HexFormat;

public final class IqlCacheKeyService {
    private final ObjectMapper mapper;
    private final String mappingVersion;
    public IqlCacheKeyService(ObjectMapper mapper, String mappingVersion) {
        this.mapper = mapper;
        this.mappingVersion = mappingVersion;
    }
    public String key(IqlQuery query) {
        IqlQuery cursorless = new IqlQuery(query.select(), query.filters(), query.filterLogic(), query.timeRange(),
                query.groupBy(), query.metrics(), query.orderBy(), query.sort(), query.size(), null,
                query.windows(), query.having(), query.derivedMetrics());
        try {
            byte[] payload = mapper.writeValueAsBytes(java.util.List.of(mappingVersion, cursorless));
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(payload));
        }
        catch (Exception e) { throw new IllegalStateException("Cannot hash IQL", e); }
    }
}
