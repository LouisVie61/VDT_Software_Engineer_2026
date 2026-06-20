package vdt.se.demo.adapter.out.llm;

import org.springframework.stereotype.Component;
import vdt.se.demo.application.dto.IntentExtractionRequest;

@Component
public class LlmIntentPromptBuilder {
    public String build(IntentExtractionRequest request) {
        String previous = request.previousIntent() == null ? "null" : request.previousIntent().toString();
        return """
                Extract SOC search intent fields from the analyst question. Return JSON only.
                Do not generate Elasticsearch DSL.
                Do not decide the final template. Extract field-level parameters only.
                Treat routing as perception metadata, not a hard gate.
                The original question is authoritative; do not rewrite it from routing hints.
                Use overrideIntent only when extracted fields clearly contradict a non-neutral routing hint.
                When routingHint.neutral=true or lowConfidencePerception=true, ignore template bias and infer directly from the original question.
                If the original question is still too ambiguous, return low confidence scores instead of forcing the nearest template.

                Allowed overrideIntent values: SIMPLE_SEARCH, TERMS_AGGREGATION, TIME_AGGREGATION.
                Allowed fields: timestamp, source, severity, event_type, action, user, host, ip, message, raw.
                Prefer structured filters for severity, event_type, action, user, host, ip.
                Convert time expressions into timeRange.from/timeRange.to.
                Do not put years, dates, "last 24h", "7 days", "7 ngay qua", "nam 2024" into textQuery.
                Do not put command words into textQuery: show, list, find, statistic, statistics, count, top, group, event, alert.
                If no real searchable keyword remains after extracting filters/time/aggregation, set textQuery to null.
                Interpret login/logon/sign-in/authentication as event_type=auth.
                For failed login/auth questions, use textQuery "failed" and filter event_type=auth.
                For successful login/auth questions, use textQuery "success" and filter event_type=auth.
                Use groupBy only when analyst asks for grouping/top/count by a field.
                If extracted fields contradict the routing hint, include overrideIntent and overrideReason.

                Response shape:
                {
                  "intent": "SIMPLE_SEARCH",
                  "textQuery": "failed",
                  "filters": {"event_type": "auth"},
                  "groupBy": "user",
                  "metric": "COUNT",
                  "topN": 10,
                  "timeBucket": "1h",
                  "timeRange": {"from": null, "to": null},
                  "overrideIntent": null,
                  "overrideReason": null,
                  "confidenceScores": {
                    "textQuery": 0.9,
                    "event_type": 0.9,
                    "groupBy": 0.8,
                    "timeRange": 0.8
                  }
                }

                Question: %s
                Routing hint: %s confidence=%s reason=%s neutral=%s lowConfidencePerception=%s
                Heuristic signal: %s confidence=%s ambiguous=%s reason=%s
                Semantic signal: %s confidence=%s ambiguous=%s reason=%s
                Previous intent for merge: %s
                MITRE enrichment: %s
                Explicit API filters:
                from=%s, to=%s, severity=%s, event_type=%s, user=%s, host=%s, ip=%s
                """.formatted(
                request.request().getQuestion(),
                request.routingHint() == null ? null : request.routingHint().templateType(),
                request.routingHint() == null ? null : request.routingHint().confidence(),
                request.routingHint() == null ? null : request.routingHint().reason(),
                request.routingHint() != null && request.routingHint().neutral(),
                request.routingHint() != null && request.routingHint().lowConfidencePerception(),
                request.heuristicHint() == null ? null : request.heuristicHint().templateType(),
                request.heuristicHint() == null ? null : request.heuristicHint().confidence(),
                request.heuristicHint() != null && request.heuristicHint().ambiguous(),
                request.heuristicHint() == null ? null : request.heuristicHint().reason(),
                request.semanticHint() == null ? null : request.semanticHint().templateType(),
                request.semanticHint() == null ? null : request.semanticHint().confidence(),
                request.semanticHint() != null && request.semanticHint().ambiguous(),
                request.semanticHint() == null ? null : request.semanticHint().reason(),
                previous,
                request.enrichments(),
                value(request.request().getFrom()),
                value(request.request().getTo()),
                value(request.request().getSeverity()),
                value(request.request().getEventType()),
                value(request.request().getUser()),
                value(request.request().getHost()),
                value(request.request().getIp())
        );
    }

    private String value(String value) {
        return value == null || value.isBlank() ? "null" : value;
    }
}

