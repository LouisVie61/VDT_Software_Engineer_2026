package vdt.se.demo.application.service.query;

import org.junit.jupiter.api.Test;
import vdt.se.demo.adapter.config.AppProperties;
import vdt.se.demo.application.dto.SearchRequest;
import vdt.se.demo.domain.model.AuditLog;
import vdt.se.demo.domain.model.CanonicalQueryPlan;
import vdt.se.demo.domain.model.ZeroResultDiagnostic;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class QueryAuditServiceTest {
    @Test
    void includesDiagnosticClassificationInSuccessAudit() {
        AtomicReference<AuditLog> saved = new AtomicReference<>();
        QueryAuditService service = new QueryAuditService(saved::set, new AppProperties());
        ZeroResultDiagnostic diagnostic = ZeroResultDiagnostic.builder()
                .originalResultTrusted(true)
                .reasonCode("NO_DATA_IN_TIME_RANGE")
                .build();

        service.success(
                UUID.randomUUID(),
                SearchRequest.builder().question("events in the last hour").build(),
                "{}",
                0,
                LocalDateTime.now(),
                CanonicalQueryPlan.builder().provider("TEST").build(),
                false,
                diagnostic
        );

        assertThat(saved.get()).isNotNull();
        assertThat(saved.get().getDiagnosticClassification()).isEqualTo("NO_DATA_IN_TIME_RANGE");
    }
}
