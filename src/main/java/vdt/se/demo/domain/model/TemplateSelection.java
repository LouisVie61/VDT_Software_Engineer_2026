package vdt.se.demo.domain.model;

import lombok.Builder;
import vdt.se.demo.domain.valueObjects.ChartType;
import vdt.se.demo.domain.valueObjects.TemplateType;

@Builder
public record TemplateSelection(
        TemplateType type,
        String groupBy,
        int size,
        ChartType chartHint,
        String reason
) {
}
