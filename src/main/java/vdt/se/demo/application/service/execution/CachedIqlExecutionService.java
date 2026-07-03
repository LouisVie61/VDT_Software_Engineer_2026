package vdt.se.demo.application.service.execution;

import tools.jackson.databind.JsonNode;
import vdt.se.demo.application.port.outboundPort.cache.BaseDslCachePort;
import vdt.se.demo.application.port.outboundPort.execution.QueryExecutorPort;
import vdt.se.demo.application.service.cache.IqlCacheKeyService;
import vdt.se.demo.application.service.compile.DslCompiler;
import vdt.se.demo.domain.iql.IqlQuery;
import vdt.se.demo.domain.model.ExecutionResult;

public final class CachedIqlExecutionService {
    private final IqlCacheKeyService keys; private final BaseDslCachePort cache; private final DslCompiler compiler; private final QueryExecutorPort executor;
    public CachedIqlExecutionService(IqlCacheKeyService keys,BaseDslCachePort cache,DslCompiler compiler,QueryExecutorPort executor){this.keys=keys;this.cache=cache;this.compiler=compiler;this.executor=executor;}
    public Executed execute(IqlQuery query){String key=keys.key(query);JsonNode dsl=cache.findBaseDsl(key).orElseGet(()->{JsonNode value=compiler.compile(query);cache.saveBaseDsl(key,value);return value;});return new Executed(key,dsl,executor.execute(dsl));}
    public record Executed(String cacheKey,JsonNode dsl,ExecutionResult result){}
}
