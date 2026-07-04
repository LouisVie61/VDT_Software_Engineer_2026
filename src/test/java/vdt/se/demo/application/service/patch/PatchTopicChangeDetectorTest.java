package vdt.se.demo.application.service.patch;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.domain.iql.IqlQuery;
import vdt.se.demo.domain.iql.PatchOperation;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PatchTopicChangeDetectorTest {
    @Test
    void flagsPatchThatRemovesPriorScopeAndAddsUnrelatedScope() {
        ObjectMapper mapper = new ObjectMapper();
        IqlQuery previous = new IqlQuery(List.of(), List.of(
                new IqlQuery.FilterCondition("f1", "severity", IqlQuery.Operator.EQ, mapper.valueToTree("high")),
                new IqlQuery.FilterCondition("f2", "source", IqlQuery.Operator.EQ, mapper.valueToTree("firewall"))),
                null, null, List.of(new IqlQuery.GroupBy("host", 10)), List.of(), null, List.of(), 50, null);
        List<PatchOperation> operations = List.of(
                new PatchOperation(PatchOperation.Type.REMOVE_FILTER, "f1", null),
                new PatchOperation(PatchOperation.Type.REMOVE_FILTER, "f2", null),
                new PatchOperation(PatchOperation.Type.ADD_FILTER, null,
                        mapper.readTree("{\"id\":\"f3\",\"field\":\"event_type\",\"op\":\"eq\",\"value\":\"login_failed\"}")),
                new PatchOperation(PatchOperation.Type.CLEAR_GROUP_BY, null, null));

        assertThat(new PatchTopicChangeDetector().likelyTopicChange(previous, operations)).isTrue();
    }
}
