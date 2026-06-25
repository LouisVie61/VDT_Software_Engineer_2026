package vdt.se.demo.adapter.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.application.port.inboundPort.QueryUseCase;
import vdt.se.demo.application.port.outboundPort.cache.DslCachePort;
import vdt.se.demo.application.port.outboundPort.cache.IntentCachePort;
import vdt.se.demo.application.port.outboundPort.history.QueryHistoryPort;
import vdt.se.demo.application.port.outboundPort.llm.IntentExtractionPort;
import vdt.se.demo.application.port.outboundPort.semantic.MitreEnrichmentPort;
import vdt.se.demo.application.service.context.ContextRetrievalService;
import vdt.se.demo.application.service.execution.SearchExecutionService;
import vdt.se.demo.application.service.intent.IntentMergeService;
import vdt.se.demo.application.service.intent.SearchIntentNormalizer;
import vdt.se.demo.application.service.query.FinalizedSearchService;
import vdt.se.demo.application.service.query.PendingConfirmationResultFactory;
import vdt.se.demo.application.service.query.QueryAuditService;
import vdt.se.demo.application.service.query.QueryConfirmationWorkflow;
import vdt.se.demo.application.service.query.QueryCsvExportService;
import vdt.se.demo.application.service.query.QueryResultPersistenceService;
import vdt.se.demo.application.service.query.QuerySearchWorkflow;
import vdt.se.demo.application.service.query.QuerySummaryService;
import vdt.se.demo.application.service.query.QueryUseCaseService;
import vdt.se.demo.application.service.query.SearchCacheContextService;
import vdt.se.demo.application.service.query.SearchDslCacheLookupService;
import vdt.se.demo.application.service.query.SearchPlanPreparationService;
import vdt.se.demo.application.service.routing.PerceptionPrefilterService;
import vdt.se.demo.application.service.routing.QueryRoutingService;
import vdt.se.demo.application.service.routing.RoutingHintPolicy;
import vdt.se.demo.application.service.template.CanonicalPlanBuilder;
import vdt.se.demo.application.service.cache.QueryHashService;

@Configuration
public class QueryApplicationConfig {
    @Bean
    QueryUseCase queryUseCase(QuerySearchWorkflow searchWorkflow,
                              QueryConfirmationWorkflow confirmationWorkflow,
                              QueryCsvExportService csvExportService,
                              QueryHistoryPort queryHistoryPort,
                              QuerySummaryService summaryService,
                              AppProperties properties) {
        return new QueryUseCaseService(searchWorkflow, confirmationWorkflow, csvExportService, queryHistoryPort, summaryService,
                properties);
    }

    @Bean
    QuerySearchWorkflow querySearchWorkflow(SearchCacheContextService cacheContextService,
                                            SearchPlanPreparationService planPreparationService,
                                            SearchDslCacheLookupService cacheLookupService,
                                            PendingConfirmationResultFactory confirmationResultFactory,
                                            FinalizedSearchService finalizedSearchService,
                                            QueryAuditService auditService) {
        return new QuerySearchWorkflow(cacheContextService, planPreparationService, cacheLookupService,
                confirmationResultFactory, finalizedSearchService, auditService);
    }

    @Bean
    SearchPlanPreparationService searchPlanPreparationService(QueryRoutingService routingService,
                                                              RoutingHintPolicy routingHintPolicy,
                                                              PerceptionPrefilterService perceptionPrefilterService,
                                                              ContextRetrievalService contextRetrievalService,
                                                              IntentExtractionPort intentExtractionPort,
                                                              MitreEnrichmentPort mitreEnrichmentPort,
                                                              IntentMergeService intentMergeService,
                                                              SearchIntentNormalizer searchIntentNormalizer,
                                                              CanonicalPlanBuilder canonicalPlanBuilder,
                                                              SearchCacheContextService cacheContextService) {
        return new SearchPlanPreparationService(routingService, routingHintPolicy, perceptionPrefilterService,
                contextRetrievalService, intentExtractionPort, mitreEnrichmentPort, intentMergeService, searchIntentNormalizer,
                canonicalPlanBuilder, cacheContextService);
    }

    @Bean
    SearchDslCacheLookupService searchDslCacheLookupService(DslCachePort dslCachePort,
                                                            SearchExecutionService searchExecutionService,
                                                            QueryResultPersistenceService persistenceService,
                                                            QueryAuditService auditService,
                                                            ObjectMapper objectMapper,
                                                            QuerySummaryService summaryService) {
        return new SearchDslCacheLookupService(dslCachePort, searchExecutionService, persistenceService,
                auditService, objectMapper, summaryService);
    }

    @Bean
    PendingConfirmationResultFactory pendingConfirmationResultFactory(IntentCachePort intentCachePort,
                                                                      ObjectMapper objectMapper) {
        return new PendingConfirmationResultFactory(intentCachePort, objectMapper);
    }

    @Bean
    QueryConfirmationWorkflow queryConfirmationWorkflow(IntentCachePort intentCachePort,
                                                        SearchIntentNormalizer searchIntentNormalizer,
                                                        CanonicalPlanBuilder canonicalPlanBuilder,
                                                        FinalizedSearchService finalizedSearchService,
                                                        QueryHashService queryHashService,
                                                        QueryAuditService auditService) {
        return new QueryConfirmationWorkflow(intentCachePort, searchIntentNormalizer, canonicalPlanBuilder,
                finalizedSearchService, queryHashService, auditService);
    }
}
