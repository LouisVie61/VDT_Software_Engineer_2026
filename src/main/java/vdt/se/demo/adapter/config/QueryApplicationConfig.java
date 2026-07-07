package vdt.se.demo.adapter.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.application.port.inboundPort.QueryUseCase;
import vdt.se.demo.application.port.outboundPort.audit.AuditLogPort;
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
import vdt.se.demo.application.service.patch.PatchTopicChangeDetector;
import vdt.se.demo.application.service.query.*;
import vdt.se.demo.application.service.reference.ReferenceResolverService;
import vdt.se.demo.application.service.validation.SchemaRegistry;

@Configuration
public class QueryApplicationConfig {
    private static final String DSL_COMPILER_CACHE_VERSION = "dsl-v2-nested-groups";

    @Bean QueryUseCase queryUseCase(IqlSearchWorkflow workflow, QueryCsvExportService csv,
                                    QueryHistoryPort history, AuditLogPort audit, QuerySummaryService summary,
                                    AppProperties properties) {
        return new QueryUseCaseService(workflow, csv, history, audit, summary, properties.getUser().getDefaultId());
    }
    @Bean IqlSearchWorkflow iqlSearchWorkflow(SessionStateStore states, IqlQueryPreparationService preparation,
            CachedIqlExecutionService execution, ResultSummaryBuilder summaries) {
        return new IqlSearchWorkflow(states, preparation, execution, summaries);
    }
    @Bean IqlQueryPreparationService iqlQueryPreparationService(LlmToolCallPort llm,
            LlmToolDefinitions tools, PatchApplierService patches, ReferenceResolverService refs,
            SchemaRegistry schema, PatchTopicChangeDetector topicChanges, IqlQueryNormalizer normalizer,
            DslCompiler compiler) {
        return new IqlQueryPreparationService(llm, tools, patches, refs, schema, topicChanges, normalizer, compiler);
    }
    @Bean CachedIqlExecutionService cachedIqlExecutionService(IqlCacheKeyService keys, BaseDslCachePort cache,
            DslCompiler compiler, QueryExecutorPort executor, ObjectMapper mapper) { return new CachedIqlExecutionService(keys, cache, compiler, executor, mapper); }
    @Bean IqlCacheKeyService iqlCacheKeyService(ObjectMapper mapper, AppProperties properties) {
        return new IqlCacheKeyService(mapper,
                properties.getSearch().getSchemaVersion() + ":" + DSL_COMPILER_CACHE_VERSION);
    }
    @Bean ResultSummaryBuilder resultSummaryBuilder(ObjectMapper mapper) { return new ResultSummaryBuilder(mapper); }
    @Bean LlmToolDefinitions llmToolDefinitions(ObjectMapper mapper) { return new LlmToolDefinitions(mapper); }
    @Bean PatchApplierService patchApplierService(ObjectMapper mapper) { return new PatchApplierService(mapper); }
    @Bean PatchTopicChangeDetector patchTopicChangeDetector() { return new PatchTopicChangeDetector(); }
    @Bean ReferenceResolverService referenceResolverService(ObjectMapper mapper) { return new ReferenceResolverService(mapper); }
    @Bean SchemaRegistry schemaRegistry() { return new SchemaRegistry(); }
    @Bean DslCompiler dslCompiler(ObjectMapper mapper) { return new DslCompiler(mapper); }
    @Bean IqlQueryNormalizer iqlQueryNormalizer(ObjectMapper mapper) { return new IqlQueryNormalizer(mapper); }
}
