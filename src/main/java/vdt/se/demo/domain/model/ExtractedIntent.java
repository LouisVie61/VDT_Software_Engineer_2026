package vdt.se.demo.domain.model;

import lombok.Builder;

@Builder
public record ExtractedIntent(
        SearchIntent intent,
        String provider,
        String rawContent
) {
}
