package vdt.se.demo.application.service.reference;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.domain.iql.IqlQuery;
import vdt.se.demo.domain.iql.ResultSummary;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import vdt.se.demo.domain.exception.BadQueryException;

class ReferenceResolverServiceTest {
    @Test
    void resolvesBucketPointerFromStructuredSummary() {
        ObjectMapper mapper = new ObjectMapper();
        IqlQuery query = new IqlQuery(List.of(), List.of(new IqlQuery.FilterCondition("f1", "ip", IqlQuery.Operator.EQ,
                mapper.readTree("{\"$ref\":\"buckets[0].key.ip\"}"))), null, null, List.of(), List.of(), null, List.of(), 50, null);
        ResultSummary summary = new ResultSummary(3, null,
                List.of(new ResultSummary.Bucket(Map.of("ip", mapper.valueToTree("10.0.0.8")), Map.of())), null);

        IqlQuery resolved = new ReferenceResolverService(mapper).resolve(query, summary);

        assertThat(resolved.filters().getFirst().value().asString()).isEqualTo("10.0.0.8");
    }

    @Test
    void staleReferenceRequiresClarification() {
        ObjectMapper mapper = new ObjectMapper();
        IqlQuery query = new IqlQuery(List.of(), List.of(new IqlQuery.FilterCondition("f1", "ip", IqlQuery.Operator.EQ,
                mapper.readTree("{\"$ref\":\"buckets[0].key.ip\"}"))), null, null, List.of(), List.of(), null, List.of(), 50, null);

        assertThatThrownBy(() -> new ReferenceResolverService(mapper).resolve(query, null))
                .isInstanceOf(BadQueryException.class)
                .extracting(error -> ((BadQueryException) error).getReasonCode())
                .isEqualTo("CLARIFICATION_REQUIRED");
    }
}
