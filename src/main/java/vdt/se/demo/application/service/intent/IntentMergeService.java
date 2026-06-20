package vdt.se.demo.application.service.intent;

import vdt.se.demo.domain.model.SearchIntent;

import java.util.Optional;

public class IntentMergeService {
    public SearchIntent merge(Optional<SearchIntent> previous, SearchIntent extracted) {
        SearchIntent merged = previous.map(SearchIntent::copy).orElseGet(SearchIntent::new);
        if (extracted == null) {
            return merged;
        }
        if (extracted.getIntent() != null) {
            merged.setIntent(extracted.getIntent());
        }
        if (hasText(extracted.getTextQuery())) {
            merged.setTextQuery(extracted.getTextQuery());
        }
        if (extracted.getFilters() != null && !extracted.getFilters().isEmpty()) {
            merged.getFilters().putAll(extracted.getFilters());
        }
        if (hasText(extracted.getGroupBy())) {
            merged.setGroupBy(extracted.getGroupBy());
        }
        if (hasText(extracted.getMetric())) {
            merged.setMetric(extracted.getMetric());
        }
        if (extracted.getTopN() != null) {
            merged.setTopN(extracted.getTopN());
        }
        if (hasText(extracted.getTimeBucket())) {
            merged.setTimeBucket(extracted.getTimeBucket());
        }
        merged.setOverrideIntent(extracted.getOverrideIntent());
        merged.setOverrideReason(extracted.getOverrideReason());
        return merged;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

