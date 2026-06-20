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
import vdt.se.demo.application.service.routing.QueryRoutingService;
import vdt.se.demo.application.service.template.CanonicalPlanBuilder;
import vdt.se.demo.domain.model.CanonicalQueryPlan;
import vdt.se.demo.domain.model.ExtractedIntent;
import vdt.se.demo.domain.model.RoutingHint;
import vdt.se.demo.domain.model.SearchIntent;
import vdt.se.demo.domain.valueObjects.TemplateType;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchPlanPreparationService {
    private static final Logger log = LoggerFactory.getLogger(SearchPlanPreparationService.class);

    private final QueryRoutingService routingService;
    private final ContextRetrievalService contextRetrievalService;
    private final IntentExtractionPort intentExtractionPort;
    private final MitreEnrichmentPort mitreEnrichmentPort;
    private final IntentMergeService intentMergeService;
    private final SearchIntentNormalizer searchIntentNormalizer;
    private final CanonicalPlanBuilder canonicalPlanBuilder;
    private final SearchCacheContextService cacheContextService;

    public SearchPlanPreparationService(QueryRoutingService routingService, ContextRetrievalService contextRetrievalService,
                                        IntentExtractionPort intentExtractionPort, MitreEnrichmentPort mitreEnrichmentPort,
                                        IntentMergeService intentMergeService, SearchIntentNormalizer searchIntentNormalizer,
                                        CanonicalPlanBuilder canonicalPlanBuilder,
                                        SearchCacheContextService cacheContextService) {
        this.routingService = routingService;
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
        log.debug("Search routing completed: heuristicType={}, heuristicConfidence={}, semanticType={}, semanticConfidence={}, effectiveType={}, effectiveConfidence={}",
                routing.heuristic().templateType(), routing.heuristic().confidence(),
                routing.semantic().templateType(), routing.semantic().confidence(),
                routing.effective().templateType(), routing.effective().confidence());

        Optional<SearchIntent> previous = contextRetrievalService.inheritedIntent(
                cache.sessionId(), cache.schemaVersion(), request.getQuestion(), routing.heuristic(), routing.semantic());
        ExtractedIntent extracted = intentExtractionPort.extract(IntentExtractionRequest.builder()
                .request(request)
                .routingHint(routing.effective())
                .previousIntent(previous.orElse(null))
                .enrichments(mitreEnrichmentPort.enrich(request.getQuestion()))
                .build());

        SearchIntent merged = searchIntentNormalizer.normalize(request,
                alignWithRoutingHint(request, intentMergeService.merge(previous, extracted.intent()), routing.effective()));
        CanonicalQueryPlan plan = canonicalPlanBuilder.build(
                cacheContextService.normalizedQuery(request),
                cache.schemaVersion(),
                cache.sessionId(),
                routing.effective(),
                extracted.intent(),
                merged,
                extracted.provider(),
                extracted.rawContent());
        return new PreparedSearchPlan(routing.effective(), extracted.provider(), plan);
    }

    private SearchIntent alignWithRoutingHint(SearchRequest request, SearchIntent intent, RoutingHint routingHint) {
        SearchIntent aligned = intent == null ? new SearchIntent() : intent;
        if (routingHint == null || routingHint.templateType() == null || routingHint.confidence() < 0.70d
                || routingHint.templateType() == TemplateType.SIMPLE_SEARCH) {
            return aligned;
        }
        aligned.setIntent(routingHint.templateType());
        if (routingHint.templateType() == TemplateType.TERMS_AGGREGATION && aligned.getTopN() == null) {
            aligned.setTopN(inferTopN(request.getQuestion()));
        }
        if (routingHint.templateType() == TemplateType.TIME_AGGREGATION && !hasText(aligned.getTimeBucket())) {
            aligned.setTimeBucket("1h");
        }
        return aligned;
    }

    private Integer inferTopN(String question) {
        Matcher matcher = Pattern.compile("\\btop\\s+(\\d+)\\b").matcher(value(question));
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 10;
    }

    private String value(String value) {
        return value == null ? "" : value.toLowerCase(java.util.Locale.ROOT);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record PreparedSearchPlan(RoutingHint routingHint, String provider, CanonicalQueryPlan plan) {
    }
}
