package vdt.se.demo.adapter.out.elasticsearch;

import org.springframework.data.elasticsearch.core.AggregationsContainer;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.application.port.outboundPort.QueryExecutorPort;
import vdt.se.demo.domain.exception.BadQueryException;
import vdt.se.demo.domain.model.ExecutionResult;
import vdt.se.demo.domain.service.EventDocumentMapper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class QueryExecutorAdapter implements QueryExecutorPort {

    private static final List<String> BUCKET_AGGREGATE_METHODS = List.of(
            "sterms", "lterms", "dterms", "dateHistogram", "histogram"
    );

    private final ElasticsearchOperations operations;
    private final ObjectMapper objectMapper;
    private final EventDocumentMapper mapper;

    public QueryExecutorAdapter(ElasticsearchOperations operations, ObjectMapper objectMapper, EventDocumentMapper mapper) {
        this.operations = operations;
        this.objectMapper = objectMapper;
        this.mapper = mapper;
    }

    @Override
    public ExecutionResult execute(JsonNode generatedDsl) {
        try {
            String dsl = objectMapper.writeValueAsString(generatedDsl);
            SearchHits<SocEventDocument> hits = operations.search(new StringQuery(dsl), SocEventDocument.class);
            List<Map<String, Object>> rows = hits.stream()
                    .map(SearchHit::getContent)
                    .map(mapper::toMap)
                    .toList();
            return new ExecutionResult(
                    rows,
                    extractAggregations(hits),
                    Math.toIntExact(Math.min(Integer.MAX_VALUE, hits.getTotalHits()))
            );
        } catch (BadQueryException e) {
            throw e;
        } catch (Exception e) {
            throw new BadQueryException("Cannot execute Elasticsearch DSL", e);
        }
    }

    private List<Map<String, Object>> extractAggregations(SearchHits<?> hits) {
        AggregationsContainer<?> aggregations = hits.getAggregations();
        if (aggregations == null) {
            return List.of();
        }

        Object nativeAggregations = invoke(aggregations, "aggregations");
        if (nativeAggregations instanceof Map<?, ?> map) {
            return extractFromMap(map);
        }
        if (nativeAggregations instanceof Iterable<?> iterable) {
            return extractFromIterable(iterable);
        }
        return List.of();
    }

    private List<Map<String, Object>> extractFromMap(Map<?, ?> aggregations) {
        List<Map<String, Object>> extracted = new ArrayList<>();
        aggregations.forEach((name, aggregate) -> extracted.addAll(extractBuckets(Objects.toString(name), aggregate)));
        return extracted;
    }

    private List<Map<String, Object>> extractFromIterable(Iterable<?> aggregations) {
        List<Map<String, Object>> extracted = new ArrayList<>();
        for (Object aggregationHolder : aggregations) {
            Object aggregation = invoke(aggregationHolder, "aggregation");
            String name = Objects.toString(invoke(aggregation, "getName"), "aggregation");
            Object aggregate = invoke(aggregation, "getAggregate");
            extracted.addAll(extractBuckets(name, aggregate));
        }
        return extracted;
    }

    private List<Map<String, Object>> extractBuckets(String aggregationName, Object aggregate) {
        if (aggregate == null) {
            return List.of();
        }

        for (String method : BUCKET_AGGREGATE_METHODS) {
            Object typedAggregate = invoke(aggregate, method);
            if (typedAggregate == null) {
                continue;
            }
            Object buckets = invoke(typedAggregate, "buckets");
            Object bucketArray = invoke(buckets, "array");
            if (bucketArray instanceof Iterable<?> iterable) {
                return bucketsToRows(aggregationName, iterable);
            }
            if (buckets instanceof Iterable<?> iterable) {
                return bucketsToRows(aggregationName, iterable);
            }
        }
        return List.of();
    }

    private List<Map<String, Object>> bucketsToRows(String aggregationName, Iterable<?> buckets) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object bucket : buckets) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("aggregation", aggregationName);
            row.put("key", bucketKey(bucket));
            row.put("count", invoke(bucket, "docCount"));
            rows.add(row);
        }
        return rows;
    }

    private Object bucketKey(Object bucket) {
        Object keyAsString = invoke(bucket, "keyAsString");
        if (keyAsString != null) {
            return keyAsString;
        }
        Object key = invoke(bucket, "key");
        Object stringValue = invoke(key, "stringValue");
        if (stringValue != null) {
            return stringValue;
        }
        Object longValue = invoke(key, "longValue");
        if (longValue != null) {
            return longValue;
        }
        Object doubleValue = invoke(key, "doubleValue");
        return doubleValue == null ? key : doubleValue;
    }

    private Object invoke(Object target, String methodName) {
        if (target == null) {
            return null;
        }
        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            return null;
        } catch (InvocationTargetException e) {
            return null;
        }
    }
}
