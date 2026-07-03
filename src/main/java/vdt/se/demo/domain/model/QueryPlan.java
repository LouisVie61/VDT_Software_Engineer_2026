package vdt.se.demo.domain.model;

import vdt.se.demo.domain.valueObjects.QueryMode;

import java.util.LinkedHashMap;
import java.util.Map;


import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QueryPlan {
    private QueryMode mode = QueryMode.SEARCH;
    private String textQuery;
    private Map<String, String> filters = new LinkedHashMap<>();
    private String sort;
    private String metric = "COUNT";
    private String groupBy;
    private Integer topN;
    private String timeBucket;

}
