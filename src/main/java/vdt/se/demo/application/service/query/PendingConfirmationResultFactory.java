package vdt.se.demo.application.service.query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.application.dto.SearchRequest;
import vdt.se.demo.application.port.outboundPort.cache.IntentCachePort;
import vdt.se.demo.domain.model.CanonicalQueryPlan;
import vdt.se.demo.domain.model.PendingConfirmation;
import vdt.se.demo.domain.model.QueryConfirmation;
import vdt.se.demo.domain.model.QueryResult;
import vdt.se.demo.domain.valueObjects.ChartType;
import vdt.se.demo.domain.valueObjects.SummaryStatus;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PendingConfirmationResultFactory {
    private static final Logger log = LoggerFactory.getLogger(PendingConfirmationResultFactory.class);

    private final IntentCachePort intentCachePort;
    private final ObjectMapper objectMapper;

    public PendingConfirmationResultFactory(IntentCachePort intentCachePort, ObjectMapper objectMapper) {
        this.intentCachePort = intentCachePort;
        this.objectMapper = objectMapper;
    }

    public QueryResult create(UUID queryId, SearchRequest request, FinalizedSearchService.CacheContext cache,
                              CanonicalQueryPlan plan) {
        String confirmationId = UUID.randomUUID().toString();
        log.debug("Saving pending confirmation: queryId={}, confirmationId={}, sessionId={}, template={}",
                queryId, confirmationId, cache.sessionId(), plan.templateSelection().type());
        intentCachePort.savePendingConfirmation(PendingConfirmation.builder()
                .confirmationId(confirmationId)
                .schemaVersion(cache.schemaVersion())
                .sessionId(cache.sessionId())
                .question(request.getQuestion())
                .from(request.getFrom())
                .to(request.getTo())
                .severity(request.getSeverity())
                .eventType(request.getEventType())
                .user(request.getUser())
                .host(request.getHost())
                .ip(request.getIp())
                .intent(plan.mergedIntent())
                .templateSelection(plan.templateSelection())
                .createdAt(Instant.now())
                .build());

        QueryResult result = baseResult(queryId, request, plan);
        result.setNeedsConfirmation(true);
        result.setConfirmation(QueryConfirmation.builder()
                .confirmationId(confirmationId)
                .intent(plan.mergedIntent())
                .templateSelection(plan.templateSelection())
                .warnings(plan.warnings())
                .requestFilters(requestFilters(request))
                .build());
        result.setWarnings(plan.warnings());
        result.setSelectedTemplate(plan.templateSelection().type().name());
        return result;
    }

    private QueryResult baseResult(UUID queryId, SearchRequest request, CanonicalQueryPlan plan) {
        return QueryResult.builder()
                .id(queryId)
                .nlQuery(request.getQuestion())
                .generatedDSL(objectMapper.createObjectNode())
                .summary("Confirmation required before executing this query.")
                .results(List.of())
                .aggregations(List.of())
                .totalCount(0)
                .chartType(ChartType.TABLE)
                .page(request.getPage())
                .pageSize(request.getPageSize())
                .summaryStatus(SummaryStatus.NOT_REQUIRED)
                .overrideIntent(plan.overrideIntent())
                .overrideReason(plan.overrideReason())
                .confidenceScores(plan.confidenceScores())
                .canonicalPlanId(queryId)
                .build();
    }

    private Map<String, String> requestFilters(SearchRequest request) {
        Map<String, String> filters = new LinkedHashMap<>();
        put(filters, "from", request.getFrom());
        put(filters, "to", request.getTo());
        put(filters, "severity", request.getSeverity());
        put(filters, "eventType", request.getEventType());
        put(filters, "user", request.getUser());
        put(filters, "host", request.getHost());
        put(filters, "ip", request.getIp());
        return filters;
    }

    private void put(Map<String, String> filters, String field, String value) {
        if (value != null && !value.isBlank()) {
            filters.put(field, value.trim());
        }
    }
}
