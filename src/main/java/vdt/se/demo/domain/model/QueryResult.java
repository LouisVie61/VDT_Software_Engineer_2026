package vdt.se.demo.domain.model;

import tools.jackson.databind.JsonNode;
import vdt.se.demo.domain.valueObjects.ChartType;
import vdt.se.demo.domain.valueObjects.SummaryStatus;


import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class QueryResult {
    private UUID id;
    private String nlQuery;
    private JsonNode generatedDSL;
    private String summary;
    private Object results;
    private Object aggregations;
    private int totalCount;
    private ChartType chartType;
    private Integer page;
    private Integer pageSize;
    private boolean needsConfirmation;
    private Object confirmation;
    @Builder.Default
    private List<SearchWarning> warnings = List.of();
    private String selectedTemplate;
    private boolean cacheHit;
    @Builder.Default
    private SummaryStatus summaryStatus = SummaryStatus.NOT_REQUIRED;
    private Object diagnostic;
    private String overrideIntent;
    private String overrideReason;
    private java.util.Map<String, Double> confidenceScores;
    private UUID canonicalPlanId;
}
