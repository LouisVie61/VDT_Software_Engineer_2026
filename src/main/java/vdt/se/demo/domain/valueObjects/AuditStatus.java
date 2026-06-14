package vdt.se.demo.domain.valueObjects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AuditStatus {
    SUCCESS,
    FAILED;

    @JsonCreator
    private static AuditStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            return FAILED;
        }

        try {
            return AuditStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return FAILED;
        }
    }

    @JsonValue
    public String jsonValue() {
        return name();
    }
}
