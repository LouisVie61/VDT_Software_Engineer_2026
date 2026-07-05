package vdt.se.demo.application.service.reference;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.domain.exception.BadQueryException;
import vdt.se.demo.domain.iql.IqlQuery;
import vdt.se.demo.domain.iql.ResultSummary;

import java.util.ArrayList;
import java.util.List;

public final class ReferenceResolverService {
    private final ObjectMapper mapper;

    public ReferenceResolverService(ObjectMapper mapper) { this.mapper = mapper; }

    public IqlQuery resolve(IqlQuery query, ResultSummary summary) {
        List<IqlQuery.FilterCondition> resolved = new ArrayList<>();
        for (IqlQuery.FilterCondition filter : query.filters()) {
            JsonNode value = filter.value();
            if (value != null && value.isObject() && value.has("$ref")) {
                if (summary == null) throw clarification("I don't have a previous result to reference. Please clarify what you want to search.");
                value = resolvePointer(mapper.valueToTree(summary), value.path("$ref").asString());
            }
            resolved.add(new IqlQuery.FilterCondition(filter.id(), filter.field(), filter.op(), value));
        }
        return new IqlQuery(query.select(), resolved, query.filterLogic(), query.timeRange(), query.groupBy(),
                query.metrics(), query.orderBy(), query.sort(), query.size(), query.pageAfter(),
                query.windows(), query.having(), query.derivedMetrics());
    }

    private JsonNode resolvePointer(JsonNode root, String reference) {
        String pointer = "/" + reference.replace("[", "/").replace("]", "").replace(".", "/");
        JsonNode value = root.at(pointer);
        if (value.isMissingNode() || value.isNull())
            throw clarification("The previous result no longer contains reference " + reference + ". Please clarify the value.");
        return value.deepCopy();
    }

    private BadQueryException clarification(String message) {
        return new BadQueryException("CLARIFICATION_REQUIRED", message);
    }
}
