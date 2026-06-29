package vdt.se.demo.application.service.intent;

import vdt.se.demo.domain.exception.BadQueryException;
import vdt.se.demo.domain.model.SearchIntent;
import vdt.se.demo.domain.valueObjects.TemplateType;

import java.util.Locale;

public class ConfirmationIntentValidator {
    private final SearchSchemaRegistry schemaRegistry;

    public ConfirmationIntentValidator(SearchSchemaRegistry schemaRegistry) {
        this.schemaRegistry = schemaRegistry;
    }

    public void validate(SearchIntent intent) {
        if (intent == null) {
            throw new BadQueryException("Confirmed intent is required");
        }
        if (intent.getTopN() != null && (intent.getTopN() < 1 || intent.getTopN() > 100)) {
            throw new BadQueryException("Top N must be between 1 and 100.");
        }
        if (!isTermsAggregation(intent)) {
            return;
        }
        String groupBy = intent.getGroupBy();
        if (groupBy == null || groupBy.isBlank()) {
            throw new BadQueryException("Aggregation requires a grouping field before execution.");
        }
        if (!schemaRegistry.isGroupable(groupBy)) {
            throw new BadQueryException("Field '" + groupBy + "' is not groupable for aggregation.");
        }
        if (intent.getTopN() == null) {
            throw new BadQueryException("Aggregation requires Top N before execution.");
        }
    }

    private boolean isTermsAggregation(SearchIntent intent) {
        if (intent.getIntent() == TemplateType.TERMS_AGGREGATION || hasText(intent.getGroupBy())) {
            return true;
        }
        return hasText(intent.getOverrideIntent())
                && TemplateType.TERMS_AGGREGATION.name().equals(
                intent.getOverrideIntent().trim().toUpperCase(Locale.ROOT));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
