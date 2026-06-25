package vdt.se.demo.domain.model;

import lombok.Builder;

@Builder
public record SemanticSpan(
        Kind kind,
        Status status,
        String text,
        String canonical,
        int start,
        int end
) {
    public enum Kind {
        OPERATION,
        TARGET,
        TEMPORAL,
        TIME_BUCKET,
        FIELD,
        FILTER,
        AUTH
    }

    public enum Status {
        RESOLVED,
        AMBIGUOUS,
        UNSUPPORTED
    }
}
