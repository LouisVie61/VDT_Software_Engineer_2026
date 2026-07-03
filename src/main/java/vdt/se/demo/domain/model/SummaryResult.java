package vdt.se.demo.domain.model;

import lombok.Builder;
import vdt.se.demo.domain.valueObjects.ChartType;
import vdt.se.demo.domain.valueObjects.SummaryStatus;

@Builder
public record SummaryResult(
        SummaryStatus status,
        String summary,
        ChartType chartType
) {
}
