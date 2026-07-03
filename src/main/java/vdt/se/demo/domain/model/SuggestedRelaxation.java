package vdt.se.demo.domain.model;

import lombok.Builder;

@Builder
public record SuggestedRelaxation(
        String action,
        String field,
        String reasonCode,
        int previewCount
) {
}
