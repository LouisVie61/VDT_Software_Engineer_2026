package vdt.se.demo.application.service.execution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import tools.jackson.databind.JsonNode;
import vdt.se.demo.application.port.outboundPort.cache.BaseDslCachePort;
import vdt.se.demo.application.port.outboundPort.execution.QueryExecutorPort;
import vdt.se.demo.application.service.cache.IqlCacheKeyService;
import vdt.se.demo.application.service.compile.DslCompiler;
import vdt.se.demo.domain.iql.IqlQuery;
import vdt.se.demo.domain.model.ExecutionResult;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import java.util.Map;

public final class CachedIqlExecutionService {
    private static final Logger log = LoggerFactory.getLogger(CachedIqlExecutionService.class);
    private final IqlCacheKeyService keys; private final BaseDslCachePort cache; private final DslCompiler compiler; private final QueryExecutorPort executor; private final ObjectMapper mapper;
    public CachedIqlExecutionService(IqlCacheKeyService keys,BaseDslCachePort cache,DslCompiler compiler,QueryExecutorPort executor,ObjectMapper mapper){this.keys=keys;this.cache=cache;this.compiler=compiler;this.executor=executor;this.mapper=mapper;}
    public Executed execute(IqlQuery query){ return execute(query, 0, query.size(), null); }
    public Executed execute(IqlQuery query, int page, int pageSize, String searchAfter) {
        JsonNode cursor = parseCursor(searchAfter);
        boolean groupedCursor = !query.groupBy().isEmpty() && cursor != null;
        IqlQuery compilationQuery = groupedCursor ? withPageAfter(query, Map.of()) : withPageAfter(query, null);
        String key = keys.key(query) + (groupedCursor ? ":composite" : ":base");
        java.util.Optional<JsonNode> cached = cache.findBaseDsl(key);
        boolean cacheHit = cached.isPresent();
        log.info("IQL DSL cache: key={}, hit={}", key, cacheHit);
        JsonNode base = cached.orElseGet(() -> { JsonNode value=compiler.compile(compilationQuery);cache.saveBaseDsl(key,value);return value; });
        ObjectNode dsl = (ObjectNode) base.deepCopy();
        applyPagination(dsl, query, page, pageSize, cursor);
        log.info("GENERATED_DSL requestId={} cacheHit={} dsl={}", requestId(), cacheHit, dsl);
        return new Executed(key,dsl,executor.execute(dsl),cacheHit);
    }

    private String requestId() {
        String value = MDC.get("requestId");
        return value == null || value.isBlank() ? "n/a" : value;
    }
    private void applyPagination(ObjectNode dsl, IqlQuery query, int page, int pageSize, JsonNode cursor) {
        int safeSize = Math.max(1, Math.min(pageSize, 500));
        if (query.groupBy().isEmpty()) {
            dsl.put("size", safeSize);
            if (cursor == null) dsl.put("from", Math.max(0, page) * safeSize);
            else {
                if (!cursor.isArray()) throw new vdt.se.demo.domain.exception.BadQueryException("searchAfter must be a JSON array for event search");
                dsl.set("search_after", cursor);
                dsl.remove("from");
            }
        } else if (cursor != null) {
            if (!cursor.isObject()) throw new vdt.se.demo.domain.exception.BadQueryException("searchAfter must be a JSON object for grouped search");
            ((ObjectNode)dsl.at("/aggs/events/composite")).set("after", cursor);
        }
    }
    private JsonNode parseCursor(String value) {
        if (value == null || value.isBlank()) return null;
        try { return mapper.readTree(value); }
        catch (RuntimeException e) { throw new vdt.se.demo.domain.exception.BadQueryException("searchAfter must be valid JSON", e); }
    }
    private IqlQuery withPageAfter(IqlQuery q, Map<String,JsonNode> after) {
        return new IqlQuery(q.select(),q.filters(),q.filterLogic(),q.timeRange(),q.groupBy(),q.metrics(),q.orderBy(),q.sort(),q.size(),after);
    }
    public record Executed(String cacheKey,JsonNode dsl,ExecutionResult result,boolean cacheHit){}
}
