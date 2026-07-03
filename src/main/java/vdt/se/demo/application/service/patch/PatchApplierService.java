package vdt.se.demo.application.service.patch;

import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.domain.exception.BadQueryException;
import vdt.se.demo.domain.iql.IqlQuery;
import vdt.se.demo.domain.iql.PatchOperation;

import java.util.ArrayList;
import java.util.List;

public final class PatchApplierService {
    private final ObjectMapper mapper;

    public PatchApplierService(ObjectMapper mapper) { this.mapper = mapper; }

    public IqlQuery apply(IqlQuery query, List<PatchOperation> operations) {
        if (query == null) throw new BadQueryException("Patch mode requires a previous query");
        IqlQuery current = query;
        for (PatchOperation operation : operations == null ? List.<PatchOperation>of() : operations) {
            current = applyOne(current, operation);
        }
        return current;
    }

    private IqlQuery applyOne(IqlQuery query, PatchOperation operation) {
        if (operation == null || operation.op() == null) throw new BadQueryException("Patch operation is missing op");
        List<IqlQuery.FilterCondition> filters = new ArrayList<>(query.filters());
        return switch (operation.op()) {
            case ADD_FILTER -> {
                IqlQuery.FilterCondition filter = convert(operation, IqlQuery.FilterCondition.class);
                if (filters.stream().anyMatch(item -> item.id().equals(filter.id())))
                    throw new BadQueryException("Filter id already exists: " + filter.id());
                filters.add(filter); yield copy(query, filters, query.groupBy(), query.timeRange(), query.metrics(), query.sort(), query.size());
            }
            case REMOVE_FILTER -> {
                boolean removed = filters.removeIf(item -> item.id().equals(operation.filterId()));
                if (!removed) throw new BadQueryException("Unknown filter id: " + operation.filterId());
                yield copy(query, filters, query.groupBy(), query.timeRange(), query.metrics(), query.sort(), query.size());
            }
            case REPLACE_FILTER -> {
                int index = indexOf(filters, operation.filterId());
                filters.set(index, convert(operation, IqlQuery.FilterCondition.class));
                yield copy(query, filters, query.groupBy(), query.timeRange(), query.metrics(), query.sort(), query.size());
            }
            case SET_GROUP_BY -> copy(query, filters, list(operation, IqlQuery.GroupBy.class), query.timeRange(), query.metrics(), query.sort(), query.size());
            case CLEAR_GROUP_BY -> copy(query, filters, List.of(), query.timeRange(), query.metrics(), query.sort(), query.size());
            case SET_TIME_RANGE -> copy(query, filters, query.groupBy(), convert(operation, IqlQuery.TimeRange.class), query.metrics(), query.sort(), query.size());
            case SET_METRICS -> copy(query, filters, query.groupBy(), query.timeRange(), list(operation, IqlQuery.Metric.class), query.sort(), query.size());
            case SET_SORT -> copy(query, filters, query.groupBy(), query.timeRange(), query.metrics(), list(operation, IqlQuery.Sort.class), query.size());
            case SET_SIZE -> copy(query, filters, query.groupBy(), query.timeRange(), query.metrics(), query.sort(), operation.value().asInt());
        };
    }

    private int indexOf(List<IqlQuery.FilterCondition> filters, String id) {
        for (int i = 0; i < filters.size(); i++) if (filters.get(i).id().equals(id)) return i;
        throw new BadQueryException("Unknown filter id: " + id);
    }

    private <T> T convert(PatchOperation operation, Class<T> type) {
        if (operation.value() == null) throw new BadQueryException(operation.op() + " requires value");
        return mapper.treeToValue(operation.value(), type);
    }

    private <T> List<T> list(PatchOperation operation, Class<T> type) {
        if (operation.value() == null || !operation.value().isArray()) throw new BadQueryException(operation.op() + " requires an array");
        List<T> result = new ArrayList<>();
        operation.value().forEach(value -> result.add(mapper.treeToValue(value, type)));
        return result;
    }

    private IqlQuery copy(IqlQuery q, List<IqlQuery.FilterCondition> filters, List<IqlQuery.GroupBy> groups,
                          IqlQuery.TimeRange range, List<IqlQuery.Metric> metrics, List<IqlQuery.Sort> sort, int size) {
        return new IqlQuery(q.select(), filters, q.filterLogic(), range, groups, metrics, q.orderBy(), sort, size, q.pageAfter());
    }
}
