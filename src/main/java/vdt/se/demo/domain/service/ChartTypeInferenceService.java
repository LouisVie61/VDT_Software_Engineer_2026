package vdt.se.demo.domain.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.domain.valueObjects.ChartType;

@Service
public class ChartTypeInferenceService {

    private static final Logger log = LoggerFactory.getLogger(ChartTypeInferenceService.class);

    private final ObjectMapper objectMapper;

    public ChartTypeInferenceService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ChartType inferChartType(String dsl) {
        try {
            JsonNode dslNode = objectMapper.readTree(dsl);
            return inferChartType(dslNode);
        } catch (Exception e) {
            log.warn("Failed to parse DSL when inferring chart type: {}", e.getMessage());
            return ChartType.TABLE;
        }
    }

    public ChartType inferChartType(JsonNode dslNode) {
        if (dslNode == null || dslNode.isNull()) {
            return ChartType.TABLE;
        }

        JsonNode aggsNode = getAggregations(dslNode);

        if (aggsNode == null || !aggsNode.isObject()) {
            return ChartType.TABLE;
        }

        for (JsonNode aggregation : aggsNode) {
            if (!aggregation.isObject()) {
                continue;
            }

            if (aggregation.has("date_histogram")) {
                return ChartType.LINE_CHART;
            }

            if (aggregation.has("terms")) {
                JsonNode terms = aggregation.get("terms");

                if (terms != null && terms.has("size")) {
                    return ChartType.BAR_CHART;
                }

                return ChartType.PIE_CHART;
            }
        }

        return ChartType.TABLE;
    }

    private JsonNode getAggregations(JsonNode dslNode) {
        if (dslNode.has("aggs")) {
            return dslNode.get("aggs");
        }

        if (dslNode.has("aggregations")) {
            return dslNode.get("aggregations");
        }

        return null;
    }
}
