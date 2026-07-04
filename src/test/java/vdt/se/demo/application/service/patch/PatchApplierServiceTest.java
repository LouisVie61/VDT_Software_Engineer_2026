package vdt.se.demo.application.service.patch;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.domain.exception.BadQueryException;
import vdt.se.demo.domain.iql.IqlQuery;
import vdt.se.demo.domain.iql.PatchOperation;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PatchApplierServiceTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final PatchApplierService service = new PatchApplierService(mapper);

    @Test
    void appliesOrderedEditsWithoutMutatingPreviousQuery() {
        IqlQuery previous = query(List.of(new IqlQuery.FilterCondition("f1", "severity", IqlQuery.Operator.EQ, mapper.valueToTree("high"))));
        PatchOperation add = new PatchOperation(PatchOperation.Type.ADD_FILTER, null,
                mapper.readTree("{\"id\":\"f2\",\"field\":\"host\",\"op\":\"EQ\",\"value\":\"web-1\"}"));
        PatchOperation remove = new PatchOperation(PatchOperation.Type.REMOVE_FILTER, "f1", null);

        IqlQuery result = service.apply(previous, List.of(add, remove));

        assertThat(result.filters()).extracting(IqlQuery.FilterCondition::id).containsExactly("f2");
        assertThat(previous.filters()).extracting(IqlQuery.FilterCondition::id).containsExactly("f1");
    }

    @Test
    void rejectsUnknownPatchTarget() {
        assertThatThrownBy(() -> service.apply(query(List.of()), List.of(
                new PatchOperation(PatchOperation.Type.REMOVE_FILTER, "missing", null))))
                .isInstanceOf(BadQueryException.class);
    }

    @Test
    void preservesEveryFieldNotAddressedByPatch() {
        IqlQuery.TimeRange range = new IqlQuery.TimeRange("timestamp", "2026-01-01T00:00:00Z", "2026-01-02T00:00:00Z");
        IqlQuery previous = new IqlQuery(List.of("event_id"), List.of(), null, range,
                List.of(new IqlQuery.GroupBy("severity", 10)), List.of(new IqlQuery.Metric(IqlQuery.MetricType.COUNT, null)),
                new IqlQuery.OrderBy(IqlQuery.OrderTarget.COUNT, null, IqlQuery.Direction.DESC),
                List.of(), 50, null);
        PatchOperation add = new PatchOperation(PatchOperation.Type.ADD_FILTER, null,
                mapper.readTree("{\"id\":\"f1\",\"field\":\"host\",\"op\":\"EQ\",\"value\":\"web-1\"}"));

        IqlQuery result = service.apply(previous, List.of(add));

        assertThat(result.select()).isSameAs(previous.select());
        assertThat(result.timeRange()).isSameAs(previous.timeRange());
        assertThat(result.groupBy()).isSameAs(previous.groupBy());
        assertThat(result.metrics()).isSameAs(previous.metrics());
        assertThat(result.orderBy()).isSameAs(previous.orderBy());
        assertThat(result.sort()).isSameAs(previous.sort());
        assertThat(result.size()).isEqualTo(previous.size());
    }

    private IqlQuery query(List<IqlQuery.FilterCondition> filters) {
        return new IqlQuery(List.of(), filters, null, null, List.of(), List.of(), null, List.of(), 50, null);
    }
}
