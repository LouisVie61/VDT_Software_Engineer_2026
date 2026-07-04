package vdt.se.demo.application.service.query;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.application.port.outboundPort.llm.LlmCallBudget;
import vdt.se.demo.application.port.outboundPort.llm.LlmToolCallPort;
import vdt.se.demo.application.service.llm.LlmToolDefinitions;
import vdt.se.demo.application.service.compile.DslCompiler;
import vdt.se.demo.application.service.patch.PatchApplierService;
import vdt.se.demo.application.service.patch.PatchTopicChangeDetector;
import vdt.se.demo.application.service.reference.ReferenceResolverService;
import vdt.se.demo.application.service.validation.SchemaRegistry;
import vdt.se.demo.domain.iql.IqlQuery;
import vdt.se.demo.domain.iql.SessionState;
import vdt.se.demo.domain.iql.ToolCallResult;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class IqlQueryPreparationServiceTest {
    @Test
    void retriesTranslatedClarificationForCompleteVietnameseCalendarRanking() {
        ObjectMapper mapper = new ObjectMapper();
        AtomicInteger calls = new AtomicInteger();
        LlmToolCallPort llm = new LlmToolCallPort() {
            public ToolCallResult invoke(String text, SessionState state, List<JsonNode> definitions) {
                throw new UnsupportedOperationException();
            }
            public ToolCallResult invoke(String text, SessionState state, List<JsonNode> definitions,
                                         List<String> errors, LlmCallBudget budget) {
                assertThat(budget.tryConsume()).isTrue();
                if (calls.getAndIncrement() == 0) {
                    return new ToolCallResult.AskClarification(ToolCallResult.Reason.UNCLEAR_INTENT,
                            "Which day in July 2025 had the most events?", List.of());
                }
                return new ToolCallResult.SearchEvents(ToolCallResult.Mode.NEW,
                        new IqlQuery(List.of(), List.of(), null,
                                new IqlQuery.TimeRange("timestamp", "2025-07-01T00:00:00Z", "2025-08-01T00:00:00Z"),
                                List.of(new IqlQuery.GroupBy("timestamp_day", 1)),
                                List.of(new IqlQuery.Metric(IqlQuery.MetricType.COUNT, null)),
                                new IqlQuery.OrderBy(IqlQuery.OrderTarget.COUNT, null, IqlQuery.Direction.DESC),
                                List.of(), 50, null), List.of());
            }
        };
        var service = new IqlQueryPreparationService(llm, new LlmToolDefinitions(mapper),
                new PatchApplierService(mapper), new ReferenceResolverService(mapper), new SchemaRegistry(),
                new PatchTopicChangeDetector(), new IqlQueryNormalizer(mapper), new DslCompiler(mapper));

        IqlQuery query = service.prepare("Trong tháng 7 năm 2025, ngày nào có nhiều sự kiện nhất?",
                new SessionState("s1", null, null, Instant.EPOCH));

        assertThat(query.groupBy().getFirst().field()).isEqualTo("timestamp_day");
        assertThat(calls).hasValue(2);
    }

    @Test
    void retriesRejectedToolCallOnceWithinSharedTwoCallBudget() {
        ObjectMapper mapper = new ObjectMapper();
        AtomicInteger calls = new AtomicInteger();
        LlmToolCallPort llm = new LlmToolCallPort() {
            public ToolCallResult invoke(String text, SessionState state, List<JsonNode> definitions) {
                throw new UnsupportedOperationException();
            }
            public ToolCallResult invoke(String text, SessionState state, List<JsonNode> definitions,
                                         List<String> errors, LlmCallBudget budget) {
                assertThat(budget.tryConsume()).isTrue();
                int size = calls.getAndIncrement() == 0 ? 501 : 50;
                return new ToolCallResult.SearchEvents(ToolCallResult.Mode.NEW,
                        new IqlQuery(List.of(), List.of(), null, null, List.of(), List.of(), null, List.of(), size, null),
                        List.of());
            }
        };
        var service = new IqlQueryPreparationService(llm, new LlmToolDefinitions(mapper),
                new PatchApplierService(mapper), new ReferenceResolverService(mapper), new SchemaRegistry(),
                new PatchTopicChangeDetector(), new IqlQueryNormalizer(mapper), new DslCompiler(mapper));

        IqlQuery query = service.prepare("show events", new SessionState("s1", null, null, Instant.EPOCH));

        assertThat(query.size()).isEqualTo(50);
        assertThat(calls).hasValue(2);
    }
}
