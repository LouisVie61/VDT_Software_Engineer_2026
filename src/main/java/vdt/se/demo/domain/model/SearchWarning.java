package vdt.se.demo.domain.model;

import lombok.Builder;

@Builder
public record SearchWarning(String code, String message) {
}
