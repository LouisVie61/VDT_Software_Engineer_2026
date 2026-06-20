package vdt.se.demo.application.service.template;

import vdt.se.demo.domain.model.CanonicalQueryPlan;
import vdt.se.demo.domain.model.IntentValidationResult;
import vdt.se.demo.domain.model.RoutingHint;
import vdt.se.demo.domain.model.SearchIntent;
import vdt.se.demo.domain.model.SemanticSpan;
import vdt.se.demo.domain.model.SearchWarning;
import vdt.se.demo.domain.model.TemplateSelection;
import vdt.se.demo.domain.valueObjects.ChartType;
import vdt.se.demo.domain.valueObjects.TemplateType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CanonicalPlanBuilder {
    private final TemplateSelectionService templateSelectionService;
    private final TemplateIntentSelector templateIntentSelector;
    private final GroupByResolver groupByResolver;

    public CanonicalPlanBuilder(TemplateSelectionService templateSelectionService,
                                TemplateIntentSelector templateIntentSelector,
                                GroupByResolver groupByResolver) {
        this.templateSelectionService = templateSelectionService;
        this.templateIntentSelector = templateIntentSelector;
        this.groupByResolver = groupByResolver;
    }

    public CanonicalQueryPlan build(String normalizedQuery, String schemaVersion, String sessionId,
                                    RoutingHint routingHint, SearchIntent extractedFields,
                                    SearchIntent mergedIntent, String provider, String rawLlmContent) {
        SearchIntent intent = mergedIntent == null ? new SearchIntent() : mergedIntent.copy();
        TemplateType selectedType = templateIntentSelector.select(routingHint, intent);
        intent.setIntent(selectedType);

        List<SearchWarning> warnings = new ArrayList<>();
        warnings.addAll(spanWarnings(intent));
        if (selectedType == TemplateType.TERMS_AGGREGATION && !hasText(intent.getGroupBy())) {
            Optional<CanonicalQueryPlan> pending = resolveMissingGroupBy(normalizedQuery, schemaVersion, sessionId,
                    routingHint, extractedFields, intent, provider, rawLlmContent, warnings);
            if (pending.isPresent()) {
                return pending.get();
            }
        }
        return validatedPlan(normalizedQuery, schemaVersion, sessionId, routingHint, extractedFields, intent,
                provider, rawLlmContent, warnings, selectedType);
    }

    private Optional<CanonicalQueryPlan> resolveMissingGroupBy(String normalizedQuery, String schemaVersion,
                                                               String sessionId, RoutingHint routingHint,
                                                               SearchIntent extractedFields, SearchIntent intent,
                                                               String provider, String rawLlmContent,
                                                               List<SearchWarning> warnings) {
        Optional<String> resolvedGroupBy = groupByResolver.resolve(normalizedQuery);
        if (resolvedGroupBy.isPresent()) {
            intent.setGroupBy(resolvedGroupBy.get());
            warnings.add(warning("GROUP_BY_RESOLVED",
                    "Aggregation grouping field resolved as '" + resolvedGroupBy.get() + "'."));
            return Optional.empty();
        }
        warnings.add(warning("GROUP_BY_REQUIRED", "Aggregation requires a grouping field before execution."));
        return Optional.of(basePlan(normalizedQuery, schemaVersion, sessionId, routingHint, extractedFields, intent,
                provider, rawLlmContent, warnings)
                .templateSelection(confirmationSelection(intent))
                .chartHint(chartHint(TemplateType.TERMS_AGGREGATION, intent))
                .build());
    }

    private CanonicalQueryPlan validatedPlan(String normalizedQuery, String schemaVersion, String sessionId,
                                             RoutingHint routingHint, SearchIntent extractedFields, SearchIntent intent,
                                             String provider, String rawLlmContent, List<SearchWarning> warnings,
                                             TemplateType selectedType) {
        IntentValidationResult validation = templateSelectionService.validate(intent, selectedType);
        if (!validation.valid()) {
            warnings.add(warning("PLAN_VALIDATION_FAILED", validation.errorMessage()));
            return basePlan(normalizedQuery, schemaVersion, sessionId, routingHint, extractedFields, intent,
                    provider, rawLlmContent, warnings).build();
        }
        warnings.addAll(validation.warnings());
        return basePlan(normalizedQuery, schemaVersion, sessionId, routingHint, extractedFields, intent,
                provider, rawLlmContent, warnings)
                .templateSelection(validation.selection())
                .chartHint(validation.selection().chartHint())
                .build();
    }

    private CanonicalQueryPlan.CanonicalQueryPlanBuilder basePlan(String normalizedQuery, String schemaVersion,
                                                                  String sessionId, RoutingHint routingHint,
                                                                  SearchIntent extractedFields,
                                                                  SearchIntent mergedIntent, String provider,
                                                                  String rawLlmContent,
                                                                  List<SearchWarning> warnings) {
        return CanonicalQueryPlan.builder()
                .normalizedQuery(normalizedQuery)
                .schemaVersion(schemaVersion)
                .sessionId(sessionId)
                .routingHint(routingHint)
                .extractedFields(extractedFields)
                .mergedIntent(mergedIntent)
                .confidenceScores(confidenceScores(mergedIntent))
                .overrideIntent(mergedIntent == null ? null : mergedIntent.getOverrideIntent())
                .overrideReason(mergedIntent == null ? null : mergedIntent.getOverrideReason())
                .semanticSpans(mergedIntent == null ? List.of() : mergedIntent.getSemanticSpans())
                .warnings(warnings)
                .provider(provider)
                .rawLlmContent(rawLlmContent);
    }

    private TemplateSelection confirmationSelection(SearchIntent intent) {
        return TemplateSelection.builder()
                .type(TemplateType.TERMS_AGGREGATION)
                .groupBy(intent == null ? null : intent.getGroupBy())
                .size(boundedSize(intent == null ? null : intent.getTopN()))
                .chartHint(chartHint(TemplateType.TERMS_AGGREGATION, intent))
                .reason("confirmation required before execution")
                .build();
    }

    private ChartType chartHint(TemplateType selectedType, SearchIntent intent) {
        if (selectedType == TemplateType.TIME_AGGREGATION) {
            return ChartType.LINE_CHART;
        }
        if (selectedType == TemplateType.TERMS_AGGREGATION) {
            return intent == null || intent.getTopN() == null ? ChartType.PIE_CHART : ChartType.BAR_CHART;
        }
        return ChartType.TABLE;
    }

    private SearchWarning warning(String code, String message) {
        return SearchWarning.builder().code(code).message(message).build();
    }

    private List<SearchWarning> spanWarnings(SearchIntent intent) {
        if (intent == null || intent.getSemanticSpans() == null) {
            return List.of();
        }
        return intent.getSemanticSpans().stream()
                .filter(span -> span.status() == SemanticSpan.Status.AMBIGUOUS
                        || span.status() == SemanticSpan.Status.UNSUPPORTED)
                .map(span -> warning("SEMANTIC_SPAN_NEEDS_REVIEW",
                        "Semantic span '" + span.text() + "' was consumed but not fully resolved."))
                .toList();
    }

    private int boundedSize(Integer topN) {
        return topN == null ? 10 : Math.max(1, Math.min(topN, 100));
    }

    private Map<String, Double> confidenceScores(SearchIntent intent) {
        return intent == null || intent.getConfidenceScores() == null ? Map.of() : intent.getConfidenceScores();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
