package vdt.se.demo.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vdt.se.demo.domain.valueObjects.TemplateType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchIntent {
    @Builder.Default
    private TemplateType intent = TemplateType.SIMPLE_SEARCH;
    private String textQuery;
    @Builder.Default
    private Map<String, String> filters = new LinkedHashMap<>();
    private String groupBy;
    @Builder.Default
    private String metric = "COUNT";
    private Integer topN;
    private String timeBucket;
    private String timeFrom;
    private String timeTo;
    private RecurringTime recurringTime;
    private String overrideIntent;
    private String overrideReason;
    @Builder.Default
    private List<SemanticSpan> semanticSpans = List.of();
    @Builder.Default
    private Map<String, Double> confidenceScores = new LinkedHashMap<>();

    public SearchIntent copy() {
        return SearchIntent.builder()
                .intent(intent)
                .textQuery(textQuery)
                .filters(filters == null ? new LinkedHashMap<>() : new LinkedHashMap<>(filters))
                .groupBy(groupBy)
                .metric(metric)
                .topN(topN)
                .timeBucket(timeBucket)
                .timeFrom(timeFrom)
                .timeTo(timeTo)
                .recurringTime(recurringTime)
                .overrideIntent(overrideIntent)
                .overrideReason(overrideReason)
                .semanticSpans(semanticSpans == null ? List.of() : List.copyOf(semanticSpans))
                .confidenceScores(confidenceScores == null ? new LinkedHashMap<>() : new LinkedHashMap<>(confidenceScores))
                .build();
    }
}
