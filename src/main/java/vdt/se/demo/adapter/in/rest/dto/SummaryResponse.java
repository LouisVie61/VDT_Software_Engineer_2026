package vdt.se.demo.adapter.in.rest.dto;

import vdt.se.demo.domain.model.SummaryResult;
import vdt.se.demo.domain.valueObjects.ChartType;
import vdt.se.demo.domain.valueObjects.SummaryStatus;

public record SummaryResponse(
        SummaryStatus status,
        String summary,
        ChartType chartType
) {
    public static SummaryResponse from(SummaryResult result) {
        return new SummaryResponse(result.status(), result.summary(), result.chartType());
    }
}
