package vdt.se.demo.application.service.query;

import vdt.se.demo.adapter.config.AppProperties;
import vdt.se.demo.application.dto.SearchRequest;
import vdt.se.demo.application.service.cache.QueryHashService;
import vdt.se.demo.domain.model.CanonicalQueryPlan;

import java.util.Locale;

public class SearchCacheContextService {
    private final QueryHashService queryHashService;
    private final AppProperties properties;

    public SearchCacheContextService(QueryHashService queryHashService, AppProperties properties) {
        this.queryHashService = queryHashService;
        this.properties = properties;
    }

    public FinalizedSearchService.CacheContext forRequest(SearchRequest request) {
        return new FinalizedSearchService.CacheContext(schemaVersion(), sessionId(request), queryHashService.hash(request));
    }

    public FinalizedSearchService.CacheContext forPlan(SearchRequest request, CanonicalQueryPlan plan) {
        return new FinalizedSearchService.CacheContext(schemaVersion(), sessionId(request), queryHashService.hash(request, plan));
    }

    public String normalizedQuery(SearchRequest request) {
        return request.getQuestion() == null ? "" : request.getQuestion().strip().toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");
    }

    private String schemaVersion() {
        return properties.getSearch().getSchemaVersion();
    }

    private String sessionId(SearchRequest request) {
        return request.getSessionId() == null || request.getSessionId().isBlank()
                ? properties.getUser().getDefaultId()
                : request.getSessionId();
    }
}
