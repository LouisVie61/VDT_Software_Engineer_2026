package vdt.se.demo.adapter.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.application.port.inboundPort.QueryUseCase;
import vdt.se.demo.application.port.outboundPort.execution.QueryExecutorPort;
import vdt.se.demo.application.port.outboundPort.cache.BaseDslCachePort;
import vdt.se.demo.application.port.outboundPort.history.QueryHistoryPort;
import vdt.se.demo.application.port.outboundPort.llm.LlmToolCallPort;
import vdt.se.demo.application.port.outboundPort.session.SessionStateStore;
import vdt.se.demo.application.service.compile.DslCompiler;
import vdt.se.demo.application.service.cache.IqlCacheKeyService;
import vdt.se.demo.application.service.execution.CachedIqlExecutionService;
import vdt.se.demo.application.service.llm.LlmToolDefinitions;
import vdt.se.demo.application.service.patch.PatchApplierService;
import vdt.se.demo.application.service.query.*;
import vdt.se.demo.application.service.reference.ReferenceResolverService;
import vdt.se.demo.application.service.validation.SchemaRegistry;

@Configuration
public class QueryApplicationConfig {
    @Bean QueryUseCase queryUseCase(IqlSearchWorkflow workflow, QueryCsvExportService csv,
                                    QueryHistoryPort history, QuerySummaryService summary, AppProperties properties) {
        return new QueryUseCaseService(workflow, csv, history, summary, properties.getUser().getDefaultId());
    }
    @Bean IqlSearchWorkflow iqlSearchWorkflow(SessionStateStore states, IqlQueryPreparationService preparation,
            CachedIqlExecutionService execution) {
        return new IqlSearchWorkflow(states, preparation, execution);
    }
    @Bean IqlQueryPreparationService iqlQueryPreparationService(LlmToolCallPort llm,
            LlmToolDefinitions tools, PatchApplierService patches, ReferenceResolverService refs,
            SchemaRegistry schema) {
        return new IqlQueryPreparationService(llm, tools, patches, refs, schema);
    }
    @Bean CachedIqlExecutionService cachedIqlExecutionService(IqlCacheKeyService keys, BaseDslCachePort cache,
            DslCompiler compiler, QueryExecutorPort executor) { return new CachedIqlExecutionService(keys, cache, compiler, executor); }
    @Bean IqlCacheKeyService iqlCacheKeyService(ObjectMapper mapper) { return new IqlCacheKeyService(mapper); }
    @Bean LlmToolDefinitions llmToolDefinitions(ObjectMapper mapper) { return new LlmToolDefinitions(mapper); }
    @Bean PatchApplierService patchApplierService(ObjectMapper mapper) { return new PatchApplierService(mapper); }
    @Bean ReferenceResolverService referenceResolverService(ObjectMapper mapper) { return new ReferenceResolverService(mapper); }
    @Bean SchemaRegistry schemaRegistry() { return new SchemaRegistry(); }
    @Bean DslCompiler dslCompiler(ObjectMapper mapper) { return new DslCompiler(mapper); }
}
