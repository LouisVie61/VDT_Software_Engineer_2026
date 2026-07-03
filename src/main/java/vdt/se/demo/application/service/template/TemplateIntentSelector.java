package vdt.se.demo.application.service.template;

import vdt.se.demo.domain.model.RoutingHint;
import vdt.se.demo.domain.model.SearchIntent;
import vdt.se.demo.domain.valueObjects.TemplateType;

import java.util.Locale;

public class TemplateIntentSelector {
    public TemplateType select(RoutingHint routingHint, SearchIntent intent) {
        if (intent == null) {
            return TemplateType.SIMPLE_SEARCH;
        }
        if (hasText(intent.getOverrideIntent())) {
            return override(intent.getOverrideIntent(), TemplateType.SIMPLE_SEARCH);
        }
        if (intent.getIntent() == TemplateType.TIME_AGGREGATION) {
            return TemplateType.TIME_AGGREGATION;
        }
        if (hasText(intent.getGroupBy())) {
            return TemplateType.TERMS_AGGREGATION;
        }
        if (intent.getIntent() == TemplateType.TERMS_AGGREGATION) {
            return TemplateType.TERMS_AGGREGATION;
        }
        return TemplateType.SIMPLE_SEARCH;
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
