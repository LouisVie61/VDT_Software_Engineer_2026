package vdt.se.demo.domain.model;

import lombok.Builder;

@Builder
public record RecurringTime(
        String mode,
        Integer month
) {
}
