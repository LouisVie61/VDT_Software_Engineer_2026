package vdt.se.demo.application.service.query;

import vdt.se.demo.application.port.outboundPort.llm.LlmToolCallPort;
import vdt.se.demo.application.port.outboundPort.llm.LlmCallBudget;
import vdt.se.demo.application.service.llm.LlmToolDefinitions;
import vdt.se.demo.application.service.compile.DslCompiler;
import vdt.se.demo.application.service.patch.PatchApplierService;
import vdt.se.demo.application.service.patch.PatchTopicChangeDetector;
import vdt.se.demo.application.service.reference.ReferenceResolverService;
import vdt.se.demo.application.service.validation.SchemaRegistry;
import vdt.se.demo.application.service.validation.ValidationResult;
import vdt.se.demo.domain.exception.BadQueryException;
import vdt.se.demo.domain.iql.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class IqlQueryPreparationService {
    private static final Logger log = LoggerFactory.getLogger(IqlQueryPreparationService.class);
    private final LlmToolCallPort llm; private final LlmToolDefinitions tools; private final PatchApplierService patches;
    private final ReferenceResolverService references; private final SchemaRegistry schema;
    private final PatchTopicChangeDetector topicChanges; private final IqlQueryNormalizer normalizer;
    private final DslCompiler compiler;

    public IqlQueryPreparationService(LlmToolCallPort llm,LlmToolDefinitions tools,PatchApplierService patches,
            ReferenceResolverService references,SchemaRegistry schema, PatchTopicChangeDetector topicChanges,
            IqlQueryNormalizer normalizer, DslCompiler compiler){this.llm=llm;this.tools=tools;this.patches=patches;this.references=references;this.schema=schema;this.topicChanges=topicChanges;this.normalizer=normalizer;this.compiler=compiler;}
    
    public IqlQuery prepare(String text, SessionState previous) {
        return prepare(text, previous, SearchConstraints.empty());
    }
    
    public IqlQuery prepare(String text, SessionState previous, SearchConstraints constraints) {
        SearchConstraints authoritative = constraints == null ? SearchConstraints.empty() : constraints;
        String llmInput = text + "\n\nAuthoritative structured constraints (do not contradict): " + authoritative;
        LlmCallBudget budget = new LlmCallBudget(4);
        java.util.List<String> correctionErrors = java.util.List.of();
        BadQueryException last = null;
        while (budget.hasRemaining()) {
            ToolCallResult call = llm.invoke(llmInput, previous, tools.all(), correctionErrors, budget);
            if (call instanceof ToolCallResult.AskClarification clarification) {
                if (repeatsRequest(text, clarification.question()) || isCompleteCalendarRanking(text)) {
                    String error = "Clarification question only repeats the analyst request. Resolve the explicit time scope and aggregation dimension, then emit search_events.";
                    last = new BadQueryException("BAD_QUERY", error);
                    correctionErrors = java.util.List.of(error);
                    log.warn("Rejected unnecessary clarification; request={}, reason={}, question={}, correction={}",
                            text, clarification.reason(), clarification.question(), error);
                    continue;
                }
                log.info("LLM requested clarification: reason={}, question={}, candidates={}",
                        clarification.reason(), clarification.question(), clarification.candidates());
                throw new BadQueryException("CLARIFICATION_REQUIRED", clarification.question());
            }
            try {
                ToolCallResult.SearchEvents search = (ToolCallResult.SearchEvents) call;
                log.info("IQL tool decision: mode={}", search.mode());
                if (search.mode() == ToolCallResult.Mode.PATCH
                        && topicChanges.likelyTopicChange(previous.lastQuery(), search.patchOps()))
                    throw new BadQueryException("Patch appears to replace the previous topic; use mode=new");
                IqlQuery query = search.mode() == ToolCallResult.Mode.PATCH
                        ? patches.apply(previous.lastQuery(), search.patchOps()) : search.query();
                query = references.resolve(query, previous.lastResultSummary());
                query = normalizer.normalize(query, authoritative);
                log.info("Resolved IQL time range: from={}, to={}", query.timeRange().from(), query.timeRange().to());
                ValidationResult result = schema.validate(query);
                if (!result.ok()) throw new BadQueryException("BAD_QUERY", String.join("; ", result.errors()), compiler.compile(query));
                if (isEmptyDsl(compiler.compile(query))) {
                    throw new BadQueryException("BAD_QUERY", "EMPTY_DSL", compiler.compile(query));
                }
                return query;
            } catch (BadQueryException rejected) {
                if ("CLARIFICATION_REQUIRED".equals(rejected.getReasonCode())) throw rejected;
                last = rejected;
                correctionErrors = java.util.List.of(rejected.getMessage());
                log.warn("IQL tool call rejected; requesting correction: reasonCode={}, validationErrors={}",
                        rejected.getReasonCode(), correctionErrors);
                if (rejected.getGeneratedDsl() != null) {
                    log.debug("Rejected generated DSL: {}", rejected.getGeneratedDsl());
                }
            }
        }
        throw last == null ? new BadQueryException("LLM call budget exhausted") : last;
    }

    private boolean isEmptyDsl(tools.jackson.databind.JsonNode dsl) {
        return dsl.path("size").asInt(-1) == 0
                && dsl.path("query").path("bool").path("filter").isArray()
                && dsl.path("query").path("bool").path("filter").isEmpty()
                && !dsl.path("aggs").isObject();
    }

    private boolean repeatsRequest(String request, String question) {
        String normalizedRequest = normalizeQuestion(request);
        String normalizedQuestion = normalizeQuestion(question);
        return !normalizedRequest.isBlank() && (normalizedRequest.equals(normalizedQuestion)
                || normalizedQuestion.contains(normalizedRequest));
    }

    private String normalizeQuestion(String value) {
        if (value == null) return "";
        return value.toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .trim();
    }

    private boolean isCompleteCalendarRanking(String request) {
        String value = normalizeQuestion(request);
        boolean hasYear = value.matches(".*\\b(?:19|20)\\d{2}\\b.*");
        boolean hasMonth = value.matches(".*\\btháng\\s+(?:[1-9]|1[0-2])\\b.*")
                || value.matches(".*\\b(?:january|february|march|april|may|june|july|august|september|october|november|december)\\b.*");
        boolean asksDay = value.contains("ngày nào") || value.contains("which day");
        boolean asksMaximum = (value.contains("nhiều") && value.contains("nhất")) || value.contains("most");
        return hasYear && hasMonth && asksDay && asksMaximum;
    }
}
