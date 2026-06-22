package vdt.se.demo.adapter.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.application.port.outboundPort.audit.AuditLogPort;
import vdt.se.demo.application.port.outboundPort.cache.DslCachePort;
import vdt.se.demo.application.port.outboundPort.cache.IntentCachePort;
import vdt.se.demo.application.port.outboundPort.execution.DiagnosticProbePort;
import vdt.se.demo.application.port.outboundPort.execution.QueryExecutionRefiner;
import vdt.se.demo.application.port.outboundPort.execution.QueryExecutorPort;
import vdt.se.demo.application.port.outboundPort.execution.SearchDslBuilderPort;
import vdt.se.demo.application.port.outboundPort.history.QueryHistoryPort;
import vdt.se.demo.application.port.outboundPort.llm.SummaryPort;
import vdt.se.demo.application.port.outboundPort.semantic.EmbeddingPort;
import vdt.se.demo.application.service.cache.QueryHashService;
import vdt.se.demo.application.service.context.ContextRetrievalService;
import vdt.se.demo.application.service.execution.SearchExecutionService;
import vdt.se.demo.application.service.intent.IntentMergeService;
import vdt.se.demo.application.service.query.DiagnosticDslVariantFactory;
import vdt.se.demo.application.service.query.FinalizedSearchService;
import vdt.se.demo.application.service.query.QueryAuditService;
import vdt.se.demo.application.service.query.QueryCsvExportService;
import vdt.se.demo.application.service.query.QueryDiagnosticService;
import vdt.se.demo.application.service.query.QueryResultPersistenceService;
import vdt.se.demo.application.service.query.QuerySummaryService;
import vdt.se.demo.application.service.query.SearchCacheContextService;
import vdt.se.demo.application.service.query.ZeroResultDiagnosticClassifier;
import vdt.se.demo.application.service.routing.PerceptionPrefilterService;
import vdt.se.demo.application.service.routing.QueryRoutingService;
import vdt.se.demo.application.service.routing.RoutingHintPolicy;
import vdt.se.demo.application.service.template.GroupByResolver;
import vdt.se.demo.domain.service.ChartTypeInferenceService;

import java.util.concurrent.Executor;

@Configuration
public class QuerySupportConfig {
    @Bean
    QuerySummaryService querySummaryService(SummaryPort summaryPort,
                                            @Qualifier("summaryTaskExecutor") Executor summaryTaskExecutor) {
        return new QuerySummaryService(summaryPort, summaryTaskExecutor);
    }

    @Bean
    QueryDiagnosticService queryDiagnosticService(DiagnosticProbePort diagnosticProbePort,
                                                  DiagnosticDslVariantFactory dslVariantFactory,
                                                  ZeroResultDiagnosticClassifier classifier,
                                                  @Qualifier("diagnosticTaskExecutor") Executor diagnosticTaskExecutor) {
        return new QueryDiagnosticService(diagnosticProbePort, dslVariantFactory, classifier, diagnosticTaskExecutor);
    }

    @Bean
    DiagnosticDslVariantFactory diagnosticDslVariantFactory(ObjectMapper objectMapper) {
        return new DiagnosticDslVariantFactory(objectMapper);
    }

    @Bean
    ZeroResultDiagnosticClassifier zeroResultDiagnosticClassifier() {
        return new ZeroResultDiagnosticClassifier();
    }

    @Bean
    QueryResultPersistenceService queryResultPersistenceService(QueryHistoryPort queryHistoryPort,
                                                                ObjectMapper objectMapper,
                                                                AppProperties properties) {
        return new QueryResultPersistenceService(queryHistoryPort, objectMapper, properties);
    }

    @Bean
    QueryCsvExportService queryCsvExportService(QueryHistoryPort queryHistoryPort, ObjectMapper objectMapper) {
        return new QueryCsvExportService(queryHistoryPort, objectMapper);
    }

    @Bean
    QueryAuditService queryAuditService(AuditLogPort auditLogPort, AppProperties properties) {
        return new QueryAuditService(auditLogPort, properties);
    }

    @Bean
    FinalizedSearchService finalizedSearchService(SearchExecutionService searchExecutionService,
                                                  QueryResultPersistenceService persistenceService,
                                                  QueryAuditService auditService,
                                                  DslCachePort dslCachePort,
                                                  IntentCachePort intentCachePort,
                                                  ObjectMapper objectMapper,
                                                  QuerySummaryService summaryService,
                                                  QueryDiagnosticService diagnosticService) {
        return new FinalizedSearchService(searchExecutionService, persistenceService, auditService, dslCachePort,
                intentCachePort, objectMapper, summaryService, diagnosticService);
    }

    @Bean
    SearchExecutionService searchExecutionService(SearchDslBuilderPort dslBuilderPort,
                                                  QueryExecutorPort queryExecutorPort,
                                                  QueryExecutionRefiner queryExecutionRefiner,
                                                  ChartTypeInferenceService chartTypeInferenceService) {
        return new SearchExecutionService(dslBuilderPort, queryExecutorPort, queryExecutionRefiner,
                chartTypeInferenceService);
    }

    @Bean
    QueryRoutingService queryRoutingService(EmbeddingPort embeddingPort) {
        return new QueryRoutingService(embeddingPort);
    }

    @Bean
    RoutingHintPolicy routingHintPolicy() {
        return new RoutingHintPolicy();
    }

    @Bean
    PerceptionPrefilterService perceptionPrefilterService(GroupByResolver groupByResolver) {
        return new PerceptionPrefilterService(groupByResolver);
    }

    @Bean
    ContextRetrievalService contextRetrievalService(IntentCachePort intentCachePort) {
        return new ContextRetrievalService(intentCachePort);
    }

    @Bean
    IntentMergeService intentMergeService() {
        return new IntentMergeService();
    }

    @Bean
    SearchCacheContextService searchCacheContextService(QueryHashService queryHashService, AppProperties properties) {
        return new SearchCacheContextService(queryHashService, properties);
    }

    @Bean
    QueryHashService queryHashService() {
        return new QueryHashService();
    }
}
