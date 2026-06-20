package vdt.se.demo.application.service.template;

import vdt.se.demo.domain.model.RoutingHint;
import vdt.se.demo.domain.model.SearchIntent;
import vdt.se.demo.domain.valueObjects.TemplateType;

import java.util.Locale;

public class TemplateIntentSelector {
    public TemplateType select(RoutingHint routingHint, SearchIntent intent) {
        TemplateType routed = routingHint == null || routingHint.templateType() == null
                ? TemplateType.SIMPLE_SEARCH
                : routingHint.templateType();
        if (intent == null) {
            return routed;
        }
        if (hasText(intent.getOverrideIntent())) {
            return override(intent.getOverrideIntent(), routed);
        }
        if (routed == TemplateType.TIME_AGGREGATION && (hasText(intent.getTimeBucket()) || hasText(intent.getTimeFrom()))) {
            return TemplateType.TIME_AGGREGATION;
        }
        if (hasText(intent.getGroupBy())) {
            return TemplateType.TERMS_AGGREGATION;
        }
        if (routed == TemplateType.TERMS_AGGREGATION) {
            return TemplateType.TERMS_AGGREGATION;
        }
        return routed;
    }

    private TemplateType override(String overrideIntent, TemplateType fallback) {
        try {
            return TemplateType.valueOf(overrideIntent.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
