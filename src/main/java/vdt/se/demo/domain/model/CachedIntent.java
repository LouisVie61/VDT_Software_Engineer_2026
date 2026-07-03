package vdt.se.demo.domain.model;

import lombok.Builder;

@Builder
public record CachedIntent(
        String schemaVersion,
        SearchIntent intent
) {
}
