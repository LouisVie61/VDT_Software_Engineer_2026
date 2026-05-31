package vdt.se.demo.domain.valueObjects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ChartType {
    TABLE("table"), // Fallback default
    LINE_CHART("line_chart"),
    BAR_CHART("bar_chart"),
    PIE_CHART("pie_chart");

    private final String type;

    ChartType(String type) {
        this.type = type;
    }

    @JsonCreator
    public static ChartType fromString(String type) {

        if (type == null || type.isBlank()) {
            return TABLE;
        }

        String normalized = type.trim().toLowerCase();
        for (ChartType chartType : values()) {
            if ((chartType.type.equals(normalized) || chartType.name().equalsIgnoreCase(normalized))) {
                return chartType;
            }
        }

        return TABLE;
    }

    @JsonValue
    public String jsonValue() {
        return type;
    }
}
