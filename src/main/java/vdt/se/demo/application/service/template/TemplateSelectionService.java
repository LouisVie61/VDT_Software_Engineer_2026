package vdt.se.demo.application.service.template;

import vdt.se.demo.domain.model.IntentValidationResult;
import vdt.se.demo.domain.model.SearchIntent;
import vdt.se.demo.domain.model.SearchWarning;
import vdt.se.demo.domain.model.TemplateSelection;
import vdt.se.demo.domain.valueObjects.ChartType;
import vdt.se.demo.domain.valueObjects.TemplateType;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public class TemplateSelectionService {
    private static final Set<String> GROUPABLE_FIELDS = Set.of(
            "timestamp", "source", "severity", "event_type", "action", "user", "host", "ip"
    );

    public IntentValidationResult validate(SearchIntent intent) {
        TemplateType type = intent == null || intent.getIntent() == null ? TemplateType.SIMPLE_SEARCH : intent.getIntent();
        return validate(intent, type);
    }

    public IntentValidationResult validate(SearchIntent intent, TemplateType type) {
        type = type == null ? TemplateType.SIMPLE_SEARCH : type;
        if (type == TemplateType.TIME_AGGREGATION) {
            return IntentValidationResult.valid(TemplateSelection.builder()
                    .type(TemplateType.TIME_AGGREGATION)
                    .groupBy("timestamp")
                    .size(boundedSize(intent == null ? null : intent.getTopN()))
                    .chartHint(ChartType.LINE_CHART)
                    .reason("time aggregation selected")
                    .build(), List.of());
        }
        if (type == TemplateType.TERMS_AGGREGATION) {
            String groupBy = normalize(intent == null ? null : intent.getGroupBy());
            if (groupBy.isBlank()) {
                return IntentValidationResult.invalid("Aggregation requires a grouping field before execution.");
            }
            if (!GROUPABLE_FIELDS.contains(groupBy) || "timestamp".equals(groupBy)) {
                return IntentValidationResult.invalid("Field '" + groupBy + "' is not groupable for aggregation.");
            }
            return IntentValidationResult.valid(TemplateSelection.builder()
                    .type(TemplateType.TERMS_AGGREGATION)
                    .groupBy(groupBy)
                    .size(boundedSize(intent.getTopN()))
                    .chartHint(intent.getTopN() == null ? ChartType.PIE_CHART : ChartType.BAR_CHART)
                    .reason("static groupable guard passed")
                    .build(), List.of());
        }
        return IntentValidationResult.valid(TemplateSelection.builder()
                .type(TemplateType.SIMPLE_SEARCH)
                .size(0)
                .chartHint(ChartType.TABLE)
                .reason("simple search selected")
                .build(), List.of(SearchWarning.builder()
                .code("ROUTER_HINT_ONLY")
                .message("Router output was used only as a hint.")
                .build()));
    }

    private int boundedSize(Integer topN) {
        if (topN == null) {
            return 10;
        }
        return Math.max(1, Math.min(topN, 100));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
