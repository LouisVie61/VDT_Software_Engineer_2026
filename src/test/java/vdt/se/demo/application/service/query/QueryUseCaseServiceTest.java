package vdt.se.demo.application.service.query;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.application.dto.SearchRequest;
import vdt.se.demo.application.port.outboundPort.audit.AuditLogPort;
import vdt.se.demo.application.port.outboundPort.history.QueryHistoryPort;
import vdt.se.demo.domain.model.AuditLog;
import vdt.se.demo.domain.model.QueryResult;
import vdt.se.demo.domain.valueObjects.AuditStatus;
import vdt.se.demo.domain.valueObjects.ChartType;
import vdt.se.demo.domain.valueObjects.SummaryStatus;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QueryUseCaseServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void schedulesBestEffortAuditAfterSuccessfulSearch() throws Exception {
        IqlSearchWorkflow workflow = mock(IqlSearchWorkflow.class);
        QueryCsvExportService csv = mock(QueryCsvExportService.class);
        QueryHistoryPort history = mock(QueryHistoryPort.class);
        AuditLogPort audit = mock(AuditLogPort.class);
        QuerySummaryService summary = mock(QuerySummaryService.class);
        QueryUseCaseService service = new QueryUseCaseService(workflow, csv, history, audit, summary, "soc-user");
        SearchRequest request = SearchRequest.builder()
                .question("find critical events")
                .sessionId("incident-1")
                .build();
        JsonNode dsl = objectMapper.readTree("{\"query\":{\"match_all\":{}}}");
        UUID queryId = UUID.randomUUID();
        QueryResult workflowResult = QueryResult.builder()
                .id(queryId)
                .nlQuery(request.getQuestion())
                .generatedDSL(dsl)
                .results(List.of(Map.of("severity", "critical")))
                .aggregations(List.of())
                .totalCount(1)
                .chartType(ChartType.TABLE)
                .selectedTemplate("IQL")
                .cacheHit(true)
                .build();
        when(workflow.search(request)).thenReturn(workflowResult);

        QueryResult result = service.search(request);

        assertThat(result.getSummaryStatus()).isEqualTo(SummaryStatus.PENDING);
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(audit).saveAsync(auditCaptor.capture());
        AuditLog auditLog = auditCaptor.getValue();
        assertThat(auditLog.getId()).isEqualTo(queryId);
        assertThat(auditLog.getUserIdentity()).isEqualTo("soc-user");
        assertThat(auditLog.getSessionId()).isEqualTo("incident-1");
        assertThat(auditLog.getNlQuery()).isEqualTo("find critical events");
        assertThat(auditLog.getGeneratedDSL()).isEqualTo("{\"query\":{\"match_all\":{}}}");
        assertThat(auditLog.getResultsCount()).isEqualTo(1);
        assertThat(auditLog.getStatus()).isEqualTo(AuditStatus.SUCCESS);
        assertThat(auditLog.getSelectedTemplate()).isEqualTo("IQL");
        assertThat(auditLog.getCacheHit()).isTrue();
    }

    @Test
    void auditFailureDoesNotFailSearchResponse() throws Exception {
        IqlSearchWorkflow workflow = mock(IqlSearchWorkflow.class);
        QueryCsvExportService csv = mock(QueryCsvExportService.class);
        QueryHistoryPort history = mock(QueryHistoryPort.class);
        AuditLogPort audit = mock(AuditLogPort.class);
        QuerySummaryService summary = mock(QuerySummaryService.class);
        QueryUseCaseService service = new QueryUseCaseService(workflow, csv, history, audit, summary, "soc-user");
        SearchRequest request = SearchRequest.builder()
                .question("find critical events")
                .sessionId("incident-1")
                .build();
        QueryResult workflowResult = QueryResult.builder()
                .id(UUID.randomUUID())
                .nlQuery(request.getQuestion())
                .generatedDSL(objectMapper.readTree("{\"query\":{\"match_all\":{}}}"))
                .results(List.of())
                .aggregations(List.of())
                .totalCount(0)
                .chartType(ChartType.TABLE)
                .selectedTemplate("IQL")
                .build();
        when(workflow.search(request)).thenReturn(workflowResult);
        doThrow(new RuntimeException("audit db down")).when(audit).saveAsync(any(AuditLog.class));

        QueryResult result = service.search(request);

        assertThat(result).isSameAs(workflowResult);
        assertThat(result.getSummaryStatus()).isEqualTo(SummaryStatus.PENDING);
        verify(audit).saveAsync(any(AuditLog.class));
    }
}
