package vdt.se.demo.application.service.execution;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.application.port.outboundPort.cache.BaseDslCachePort;
import vdt.se.demo.application.service.cache.IqlCacheKeyService;
import vdt.se.demo.application.service.compile.DslCompiler;
import vdt.se.demo.domain.iql.IqlQuery;
import vdt.se.demo.domain.model.ExecutionResult;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CachedIqlExecutionServiceTest {
    @Test
    void appliesPaginationAfterBaseDslCacheLookup() {
        ObjectMapper mapper = new ObjectMapper();
        AtomicReference<JsonNode> cached = new AtomicReference<>();
        BaseDslCachePort cache = new BaseDslCachePort() {
            public Optional<JsonNode> findBaseDsl(String key) { return Optional.ofNullable(cached.get()); }
            public void saveBaseDsl(String key, JsonNode dsl) { cached.set(dsl); }
        };
        AtomicReference<JsonNode> executed = new AtomicReference<>();
        var service = new CachedIqlExecutionService(new IqlCacheKeyService(mapper, "v1"), cache,
                new DslCompiler(mapper), dsl -> { executed.set(dsl); return new ExecutionResult(List.of(), List.of(), 0); }, mapper);
        IqlQuery query = new IqlQuery(List.of(), List.of(), null,
                new IqlQuery.TimeRange("timestamp", "2026-07-03T00:00:00Z", "2026-07-04T00:00:00Z"),
                List.of(), List.of(), null, List.of(), 50, null);

        var first = service.execute(query, 1, 25, null);
        assertThat(first.cacheHit()).isFalse();
        assertThat(executed.get().path("from").asInt()).isEqualTo(25);
        assertThat(executed.get().path("size").asInt()).isEqualTo(25);

        var second = service.execute(query, 2, 10, null);
        assertThat(second.cacheHit()).isTrue();
        assertThat(executed.get().path("from").asInt()).isEqualTo(20);
        assertThat(executed.get().path("size").asInt()).isEqualTo(10);
        assertThat(cached.get().has("from")).isFalse();
    }
}
