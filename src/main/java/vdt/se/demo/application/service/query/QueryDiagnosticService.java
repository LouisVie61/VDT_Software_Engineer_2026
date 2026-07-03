package vdt.se.demo.application.service.query;

import tools.jackson.databind.JsonNode;
import vdt.se.demo.application.dto.SearchRequest;
import vdt.se.demo.application.port.outboundPort.execution.DiagnosticProbePort;
import vdt.se.demo.domain.model.CanonicalQueryPlan;
import vdt.se.demo.domain.model.DiagnosticProbeResult;
import vdt.se.demo.domain.model.ZeroResultDiagnostic;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class QueryDiagnosticService {
    private final DiagnosticProbePort diagnosticProbePort;
    private final DiagnosticDslVariantFactory dslVariantFactory;
    private final ZeroResultDiagnosticClassifier classifier;
    private final Executor diagnosticTaskExecutor;

    public QueryDiagnosticService(DiagnosticProbePort diagnosticProbePort,
                                  DiagnosticDslVariantFactory dslVariantFactory,
                                  ZeroResultDiagnosticClassifier classifier,
                                  Executor diagnosticTaskExecutor) {
        this.diagnosticProbePort = diagnosticProbePort;
        this.dslVariantFactory = dslVariantFactory;
        this.classifier = classifier;
        this.diagnosticTaskExecutor = diagnosticTaskExecutor;
    }

    public ZeroResultDiagnostic diagnose(SearchRequest request, CanonicalQueryPlan plan, JsonNode originalDsl) {
        List<CompletableFuture<DiagnosticProbeResult>> futures = List.of(
                probe("P1", "time range only", dslVariantFactory.timeRangeOnly(originalDsl)),
                probe("P2", "core semantic filters only", dslVariantFactory.coreFiltersOnly(originalDsl)),
                probe("P3", "low-confidence filters removed",
                        dslVariantFactory.withoutLowConfidenceFilters(originalDsl, plan.confidenceScores())),
                probe("P4", "full-text terms removed", dslVariantFactory.withoutFullText(originalDsl)),
                probe("P5", "time range widened", dslVariantFactory.withoutTimeRange(originalDsl))
        );
        List<DiagnosticProbeResult> results = futures.stream().map(CompletableFuture::join).toList();
        return classifier.classify(results);
    }

    private CompletableFuture<DiagnosticProbeResult> probe(String name, String description, JsonNode dsl) {
        return CompletableFuture.supplyAsync(() -> DiagnosticProbeResult.builder()
                .probe(name)
                .description(description)
                .count(diagnosticProbePort.count(dslVariantFactory.countDsl(dsl)))
                .build(), diagnosticTaskExecutor);
    }
}
