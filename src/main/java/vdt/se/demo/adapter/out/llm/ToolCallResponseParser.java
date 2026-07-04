package vdt.se.demo.adapter.out.llm;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.domain.exception.LlmException;
import vdt.se.demo.domain.iql.IqlQuery;
import vdt.se.demo.domain.iql.PatchOperation;
import vdt.se.demo.domain.iql.ToolCallResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class ToolCallResponseParser {
    private final ObjectMapper mapper;
    ToolCallResponseParser(ObjectMapper mapper) { this.mapper = mapper; }

    ToolCallResult parse(String raw) {
        try {
            JsonNode root = mapper.readTree(stripFence(raw));
            JsonNode args = requiredObject(root, "arguments");
            return switch (requiredText(root, "name")) {
                case "ask_clarification" -> clarification(args);
                case "search_events" -> search(args);
                default -> throw new LlmException("Unsupported LLM tool name: " + root.path("name").asString("<missing>"));
            };
        } catch (LlmException e) { throw e; }
        catch (RuntimeException e) { throw new LlmException("Malformed LLM tool call: " + rootMessage(e), e); }
    }

    private ToolCallResult clarification(JsonNode args) {
        return new ToolCallResult.AskClarification(enumValue(ToolCallResult.Reason.class, requiredText(args, "reason")),
                requiredText(args, "question"), strings(args.path("candidates")));
    }

    private ToolCallResult search(JsonNode args) {
        ToolCallResult.Mode mode = enumValue(ToolCallResult.Mode.class, requiredText(args, "mode"));
        if (mode == ToolCallResult.Mode.PATCH) return new ToolCallResult.SearchEvents(mode, null, patches(args.path("patch_ops")));
        if (args.has("query")) throw new LlmException("search_events fields must be direct children of arguments; unexpected arguments.query");
        optionalArray(args, "select"); optionalArray(args, "filters"); optionalArray(args, "group_by");
        optionalArray(args, "metrics"); optionalArray(args, "sort");
        return new ToolCallResult.SearchEvents(mode, query(args), List.of());
    }

    private IqlQuery query(JsonNode node) {
        List<IqlQuery.FilterCondition> filters = new ArrayList<>();
        node.path("filters").forEach(item -> filters.add(new IqlQuery.FilterCondition(requiredText(item,"id"),
                requiredText(item,"field"), enumValue(IqlQuery.Operator.class, requiredText(item,"op")), decodedValue(item.get("value")))));
        List<IqlQuery.GroupBy> groups = new ArrayList<>();
        node.path("group_by").forEach(item -> groups.add(new IqlQuery.GroupBy(requiredText(item,"field"), integer(item,"size"))));
        List<IqlQuery.Metric> metrics = new ArrayList<>();
        node.path("metrics").forEach(item -> metrics.add(new IqlQuery.Metric(enumValue(IqlQuery.MetricType.class,
                requiredText(item,"type")), text(item,"field"))));
        List<IqlQuery.Sort> sorts = new ArrayList<>();
        node.path("sort").forEach(item -> sorts.add(new IqlQuery.Sort(requiredText(item,"field"),
                enumValue(IqlQuery.Direction.class, requiredText(item,"order")))));
        JsonNode time=node.path("time_range");
        IqlQuery.TimeRange range=time.isObject()?new IqlQuery.TimeRange(text(time,"field"),text(time,"from"),text(time,"to")):null;
        JsonNode order=node.path("order_by");
        IqlQuery.OrderBy orderBy=order.isObject()&&!order.isEmpty()?new IqlQuery.OrderBy(enumValue(IqlQuery.OrderTarget.class,
                requiredText(order,"target")),integer(order,"metric_index"),enumValue(IqlQuery.Direction.class,requiredText(order,"direction"))):null;
        return new IqlQuery(strings(node.path("select")),filters,null,range,groups,metrics,orderBy,sorts,
                node.path("size").isNumber()?node.path("size").asInt():50,pageAfter(node.path("page_after")));
    }

    private List<PatchOperation> patches(JsonNode array) {
        if (!array.isArray()) throw new LlmException("patch mode requires patch_ops");
        List<PatchOperation> result=new ArrayList<>();
        array.forEach(item -> result.add(new PatchOperation(enumValue(PatchOperation.Type.class,requiredText(item,"op")),
                text(item,"filter_id"),decodedValue(item.get("value"))))); return result;
    }
    private Map<String,JsonNode> pageAfter(JsonNode node) {
        if (!node.isObject()) return null; Map<String,JsonNode> result=new LinkedHashMap<>(); node.properties().forEach(e->result.put(e.getKey(),e.getValue())); return result;
    }
    private JsonNode requiredObject(JsonNode node,String field){JsonNode v=node.path(field);if(!v.isObject())throw new LlmException(field+" must be an object");return v;}
    private void optionalArray(JsonNode node,String field){if(node.has(field)&&!node.path(field).isArray())throw new LlmException(field+" must be an array");}
    private String requiredText(JsonNode node,String field){String v=text(node,field);if(v==null||v.isBlank())throw new LlmException(field+" is required");return v;}
    private String text(JsonNode node,String field){JsonNode v=node.path(field);return v.isString()?v.asString():null;}
    private Integer integer(JsonNode node,String field){JsonNode v=node.path(field);return v.isNumber()?v.asInt():null;}
    private List<String> strings(JsonNode node){List<String> r=new ArrayList<>();if(node.isArray())node.forEach(v->{if(v.isString())r.add(v.asString());});return r;}
    private JsonNode decodedValue(JsonNode value){
        if(value==null||!value.isString())return value;
        String raw=value.asString();
        try{return mapper.readTree(raw);}catch(RuntimeException ignored){return value;}
    }
    private <E extends Enum<E>> E enumValue(Class<E> type,String value){return Enum.valueOf(type,value.toUpperCase(Locale.ROOT));}
    private String stripFence(String value){if(value==null)throw new LlmException("Empty LLM response");return value.strip().replaceFirst("(?s)^```(?:json)?\\s*","").replaceFirst("\\s*```$","");}
    private String rootMessage(Throwable failure){Throwable root=failure;while(root.getCause()!=null)root=root.getCause();return root.getMessage()==null?root.getClass().getSimpleName():root.getMessage();}
}
