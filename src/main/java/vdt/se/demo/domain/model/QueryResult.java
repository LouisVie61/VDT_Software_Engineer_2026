package vdt.se.demo.domain.model;

import tools.jackson.databind.JsonNode;
import vdt.se.demo.domain.valueObjects.ChartType;


import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
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
}
