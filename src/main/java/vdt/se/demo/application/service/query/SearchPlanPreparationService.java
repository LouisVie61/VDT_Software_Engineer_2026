package vdt.se.demo.application.service.query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vdt.se.demo.application.dto.IntentExtractionRequest;
import vdt.se.demo.application.dto.SearchRequest;
import vdt.se.demo.application.port.outboundPort.llm.IntentExtractionPort;
import vdt.se.demo.application.port.outboundPort.semantic.MitreEnrichmentPort;
import vdt.se.demo.application.service.context.ContextRetrievalService;
import vdt.se.demo.application.service.intent.IntentMergeService;
import vdt.se.demo.application.service.intent.SearchIntentNormalizer;
import vdt.se.demo.application.service.routing.RoutingHintPolicy;
import vdt.se.demo.application.service.routing.QueryRoutingService;
import vdt.se.demo.application.service.template.CanonicalPlanBuilder;
import vdt.se.demo.domain.model.CanonicalQueryPlan;
import vdt.se.demo.domain.model.ExtractedIntent;
import vdt.se.demo.domain.model.RoutingHint;
import vdt.se.demo.domain.model.SearchIntent;

import java.util.Optional;

public class SearchPlanPreparationService {
    private static final Logger log = LoggerFactory.getLogger(SearchPlanPreparationService.class);

    private final QueryRoutingService routingService;
    private final RoutingHintPolicy routingHintPolicy;
    private final ContextRetrievalService contextRetrievalService;
    private final IntentExtractionPort intentExtractionPort;
    private final MitreEnrichmentPort mitreEnrichmentPort;
    private final IntentMergeService intentMergeService;
    private final SearchIntentNormalizer searchIntentNormalizer;
    private final CanonicalPlanBuilder canonicalPlanBuilder;
    private final SearchCacheContextService cacheContextService;

    public SearchPlanPreparationService(QueryRoutingService routingService, RoutingHintPolicy routingHintPolicy,
                                        ContextRetrievalService contextRetrievalService,
                                        IntentExtractionPort intentExtractionPort, MitreEnrichmentPort mitreEnrichmentPort,
                                        IntentMergeService intentMergeService, SearchIntentNormalizer searchIntentNormalizer,
                                        CanonicalPlanBuilder canonicalPlanBuilder,
                                        SearchCacheContextService cacheContextService) {
        this.routingService = routingService;
        this.routingHintPolicy = routingHintPolicy;
        this.contextRetrievalService = contextRetrievalService;
        this.intentExtractionPort = intentExtractionPort;
        this.mitreEnrichmentPort = mitreEnrichmentPort;
        this.intentMergeService = intentMergeService;
        this.searchIntentNormalizer = searchIntentNormalizer;
        this.canonicalPlanBuilder = canonicalPlanBuilder;
        this.cacheContextService = cacheContextService;
    }

    public PreparedSearchPlan prepare(SearchRequest request, FinalizedSearchService.CacheContext cache) {
        QueryRoutingService.RoutingDecision routing = routingService.route(request.getQuestion());
        log.debug("Search routing completed: heuristicType={}, heuristicConfidence={}, semanticType={}, semanticConfidence={}",
                routing.heuristic().templateType(), routing.heuristic().confidence(),
                routing.semantic().templateType(), routing.semantic().confidence());

        Optional<SearchIntent> previous = contextRetrievalService.inheritedIntent(
                cache.sessionId(), cache.schemaVersion(), request.getQuestion(), routing.heuristic(), routing.semantic());
        RoutingHint llmHint = routingHintPolicy.hintForLlm(routing, previous.orElse(null));
        ExtractedIntent extracted = intentExtractionPort.extract(IntentExtractionRequest.builder()
                .request(request)
                .routingHint(llmHint)
                .heuristicHint(routing.heuristic())
                .semanticHint(routing.semantic())
                .previousIntent(previous.orElse(null))
                .enrichments(mitreEnrichmentPort.enrich(request.getQuestion()))
                .build());

        SearchIntent merged = searchIntentNormalizer.normalize(request,
                intentMergeService.merge(previous, extracted.intent()));
        CanonicalQueryPlan plan = canonicalPlanBuilder.build(
                cacheContextService.normalizedQuery(request),
                cache.schemaVersion(),
                cache.sessionId(),
                llmHint,
                extracted.intent(),
                merged,
                extracted.provider(),
                extracted.rawContent());
        return new PreparedSearchPlan(llmHint, extracted.provider(), plan);
    }

    public record PreparedSearchPlan(RoutingHint routingHint, String provider, CanonicalQueryPlan plan) {
    }
}
