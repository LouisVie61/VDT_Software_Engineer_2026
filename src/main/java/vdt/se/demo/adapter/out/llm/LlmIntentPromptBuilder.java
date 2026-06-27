package vdt.se.demo.adapter.out.llm;

import org.springframework.stereotype.Component;
import vdt.se.demo.application.dto.IntentExtractionRequest;

import java.time.Instant;

@Component
public class LlmIntentPromptBuilder {

    public String systemPrompt() {
        return """
                You are a SOC intent extraction engine.

                Your job is to interpret the analyst's natural-language question
                and extract a normalized field-level intent JSON object.

                You handle linguistic interpretation:
                - Vietnamese, English, and mixed-language phrasing
                - typos and informal analyst wording
                - temporal expressions
                - ambiguous SOC descriptions
                - follow-up questions when context is relevant

                You must NOT generate Elasticsearch DSL.
                You must NOT execute queries.
                You must NOT act as an authoritative router.

                Perception/routing signals are soft priors only.
                Explicit API filters and the analyst's current question are stronger evidence.

                Return exactly one valid JSON object.
                No markdown.
                No explanation.
                No wrapper object.
                """;
    }

    public String build(IntentExtractionRequest request) {
        String previous = request.previousIntent() == null
                ? "null"
                : request.previousIntent().toString();

        return """
                Task:
                Extract normalized SOC search intent fields from the analyst question.

                The application layer will:
                - decide whether previous context should be merged
                - validate fields and values
                - select the final template
                - build the canonical Elasticsearch query plan
                - build Elasticsearch DSL
                - enforce safety constraints

                Your output is only a candidate intent extraction.

                === CORE CONTRACT ===
                Linguistic interpretation happens in this LLM call.
                Deterministic validation and execution guarantees happen outside this call.

                Do not rely on downstream rule-based preprocessing to:
                - remove command words
                - infer natural-language filters
                - infer authentication/login meaning
                - infer top N
                - infer time expressions
                - disambiguate Vietnamese or English wording

                Use the injected context to resolve ambiguity.
                If ambiguity remains, expose it through:
                - confidenceScores
                - semanticSpans
                - unresolvedAmbiguities

                Do not force a template when the question is underspecified.

                === CURRENT DATETIME ===
                Now: %s

                Resolve relative and calendar time expressions to absolute ISO 8601 timestamps when possible.
                Never output relative time strings such as "now-7d", "last week", or "today".

                Calendar rules:
                - Quarter 1 = Jan 01 to Mar 31
                - Quarter 2 = Apr 01 to Jun 30
                - Quarter 3 = Jul 01 to Sep 30
                - Quarter 4 = Oct 01 to Dec 31
                - "thang 7 hang nam", "thang 7 moi nam", "July every year", and similar annual recurring
                  month/date phrases are recurrence constraints, not a concrete time range.
                  Do not resolve them to the next/current calendar year such as July 2026.
                  Leave timeRange.from and timeRange.to null and set recurringTime={"mode":"EVERY_YEAR","month":7}
                  unless the user names a concrete year.
                - "tuan truoc" means past 7 days from now
                - "hom nay" means start of today UTC to now
                - "thang nay" means first day of current month to now
                - "dau nam" means January 1 of current year to now
                - "hom qua" means start of yesterday UTC to end of yesterday UTC

                === ALLOWED INTENTS ===
                intent must be one of:
                - SIMPLE_SEARCH
                - TERMS_AGGREGATION
                - TIME_AGGREGATION
                - UNRESOLVABLE

                Use SIMPLE_SEARCH when the analyst asks to find, show, list, retrieve, or inspect events.
                Use TERMS_AGGREGATION when the analyst asks for top, count, group by, statistics by field.
                Use TIME_AGGREGATION when the analyst asks for trend, over time, per hour, per day, timeline.

                allowed overrideIntent values:
                - SIMPLE_SEARCH
                - TERMS_AGGREGATION
                - TIME_AGGREGATION
                - null

                Use overrideIntent only when extracted fields clearly contradict a non-neutral routing hint.
                Do not override TIME_AGGREGATION to TERMS_AGGREGATION merely because the question says
                "statistics events" or "thong ke event"; explicit grouping/top/frequency wording is required.
                Do not explain the override outside JSON.

                === ALLOWED FIELDS ===
                %s

                Structured filters may only use:
                %s

                textQuery may only represent searchable free text for the message and user_agent fields.
                Never use raw.

                Location field contract:
                - Natural-language location/country/city/province/region/state/address mentions must use filters.geo_location.
                - Never output filters named location, country, city, province, region, state, or address.
                - Examples: "at Vietnam", "in Vietnam", "tai Vietnam", "o Vietnam" -> filters.geo_location = "Vietnam".
                - Do not add geo_location merely because the query is written in Vietnamese, mentions a language,
                  or references Vietnam as non-filter context. Use geo_location only for an explicit location constraint.

                === KNOWN FIELD VALUES ===
                severity values:
                - critical
                - high
                - medium
                - low
                - info

                event_type examples:
                - auth
                - failed_login
                - malware
                - network
                - process
                - file
                - alert

                Use exact known values for structured filters only when semantically justified.
                If a user description maps clearly to a known event_type, use that event_type filter.
                If no known value clearly matches, use textQuery instead of inventing a field value.

                === EXPLICIT API FILTERS ===
                Explicit API filters have highest priority for the same field.
                If an explicit API filter is present and not null/blank/"null":
                - include it in filters or timeRange
                - do not emit a conflicting extracted value for that same field

                Omit null, blank, and literal "null" values.

                Explicit API filters:
                from=%s
                to=%s
                severity=%s
                event_type=%s
                user=%s
                host=%s
                ip=%s

                === LINGUISTIC MAPPING GUIDANCE ===
                Prefer structured filters for:
                %s

                Location phrases map to geo_location:
                - at/in/o/tai/tại/ở + country/city/province/region/state/address -> geo_location
                - country/location/city/province/region/state/address are semantic aliases, not output field names

                Valid IP addresses should become ip filters.
                Do not put IP addresses into textQuery unless the analyst explicitly says the message contains that IP.

                Do not put time expressions into textQuery.
                Examples to exclude from textQuery:
                - last 24h
                - 7 days
                - 7 ngay qua
                - hom nay
                - tuan truoc
                - dau nam
                - Q1
                - nam 2024
                - yesterday

                Do not put command/UI words into textQuery.
                Examples:
                - show
                - list
                - find
                - get
                - log
                - logs
                - event
                - events
                - alert
                - alerts
                - display
                - give
                - tell
                - cho toi
                - hien thi
                - tim
                - lay

                Do not remove "error" or "errors" when they are part of a meaningful technical phrase.
                Examples:
                - timeout error
                - authentication error
                - connection error
                - permission error

                If no meaningful searchable text remains after extracting filters, time, and aggregation fields:
                set textQuery to null.

                Authentication/login mapping:
                - login, logon, sign-in, authentication, dang nhap generally indicate authentication-related events.
                - If known event_type contains failed_login and the analyst asks failed login/login failed/dang nhap that bai:
                  use filters.event_type = "failed_login" and textQuery = null unless extra failure details remain.
                - Otherwise, if only auth is available:
                  use filters.event_type = "auth" and textQuery = "failed" for failed login/auth questions.
                - For successful login/auth questions:
                  use filters.event_type = "auth" when applicable and textQuery = "success" only if no more specific event_type exists.

                Attack/intrusion mapping:
                - attack, tan cong, xam nhap, intrusion may be textQuery if no exact event_type exists.
                - If a known event_type clearly matches the concept, use event_type instead.

                === AGGREGATION RULES ===
                Use groupBy only when the analyst explicitly asks for grouping, top, most frequent, count by field,
                statistics by field, or distribution by field.

                Do not infer groupBy=event_type merely because the user asks for statistics/events.
                "statistics events", "statistics of events", "thong ke event", or "thong ke su kien" alone means
                aggregate event counts according to the strongest other signal, not group by event_type.
                Only set groupBy when the user explicitly asks "by type", "by source", "by severity", "top",
                "most frequent", "group by", "theo loai", "theo nguon", "theo muc do", or "nhieu nhat".
                If the user asks for statistics over a time period without explicit grouping, prefer
                TIME_AGGREGATION with a suitable timeBucket.

                groupBy may only be one of:
                %s

                Infer groupBy only when explicit:
                - top IPs -> ip
                - top users -> user
                - top hosts -> host
                - top locations/countries/addresses -> geo_location
                - top user agents/browsers/clients -> user_agent
                - top events -> event_type
                - by source -> source
                - by severity -> severity
                - by event type -> event_type
                - by action -> action
                - by geo location/location/country/address -> geo_location
                - by user agent/browser/client -> user_agent
                - statistics by event type -> event_type
                - thong ke theo loai su kien -> event_type
                - distribution by event type -> event_type

                If intent is TERMS_AGGREGATION but groupBy cannot be determined:
                return intent = "UNRESOLVABLE" with reason = "missing aggregation field".

                If top N is requested, set topN to that number.
                If top/count/grouping is requested but N is not specified, set topN to 10.
                If the query is not a terms aggregation, set topN to null unless explicitly meaningful.

                Use metric = "COUNT" for count/statistics/top/group-by queries.
                Otherwise metric should be null.

                Time aggregation:
                - Use TIME_AGGREGATION when analyst asks trend, timeline, over time, per hour, per day, per week.
                - timeBucket may only be "1h", "1d", or "1w".
                - Use "1h" for ranges <= 2 days or when user asks per hour.
                - Use "1d" for ranges > 2 days and <= 90 days or when user asks per day.
                - Use "1w" for ranges > 90 days or when user asks per week.
                - Default to "1d" when unclear.
                - For TIME_AGGREGATION, groupBy should be null unless the analyst explicitly asks for both time trend and grouping.

                === PREVIOUS CONTEXT RULE ===
                Previous intent is provided only as context.
                Do not blindly merge it.

                Use previous context only when the current question is a follow-up, elliptical, or incomplete.
                Examples:
                - "filter to last week"
                - "only critical"
                - "group by source"
                - "show top IPs instead"
                - "what about today?"

                If the current question contains a complete new subject, do not inherit unrelated previous filters.

                Output contextUsage:
                - "USED" when previous context is needed to understand the current question
                - "IGNORED" when current question is standalone
                - "NOT_AVAILABLE" when previous context is null

                === PERCEPTION LAYER CONTRACT ===
                Routing hints are:
                - soft priors
                - observability signals
                - optimization metadata

                Routing hints are never authoritative.
                The original question and explicit API filters are stronger evidence.

                When routingHint.neutral=true or lowConfidencePerception=true:
                - ignore template bias
                - infer directly from the original question

                If extracted fields contradict the routing hint:
                - include overrideIntent
                - include a brief overrideReason

                If no contradiction exists:
                - overrideIntent = null
                - overrideReason = null

                Routing hint:
                template=%s
                confidence=%s
                reason=%s
                neutral=%s
                lowConfidencePerception=%s

                Heuristic signal:
                template=%s
                confidence=%s
                ambiguous=%s
                reason=%s

                Semantic signal:
                template=%s
                confidence=%s
                ambiguous=%s
                reason=%s

                Previous intent:
                %s

                MITRE enrichment:
                %s

                Use MITRE enrichment only when:
                - the field is allowed
                - the value is valid for known-value keyword fields
                - it does not conflict with explicit API filters

                === REQUIRED RESPONSE SHAPE ===
                Return exactly this JSON shape.
                Omit no top-level keys.
                Use null for unknown values.

                {
                  "intent": "SIMPLE_SEARCH",
                  "reason": null,
                  "textQuery": null,
                  "filters": {
                    "source": null,
                    "severity": null,
                    "event_type": null,
                    "action": null,
                    "user": null,
                    "host": null,
                    "ip": null,
                    "geo_location": null,
                    "user_agent": null
                  },
                  "groupBy": null,
                  "metric": null,
                  "topN": null,
                  "timeBucket": null,
                  "timeRange": {
                    "from": null,
                    "to": null
                  },
                  "recurringTime": null,
                  "contextUsage": "NOT_AVAILABLE",
                  "overrideIntent": null,
                  "overrideReason": null,
                  "semanticSpans": [
                    {
                      "kind": "TEMPORAL",
                      "status": "RESOLVED",
                      "text": "last 24h",
                      "canonical": "2026-06-19T00:00:00Z/2026-06-20T00:00:00Z",
                      "start": 10,
                      "end": 18
                    }
                  ],
                  "unresolvedAmbiguities": [],
                  "confidenceScores": {
                    "intent": 0.0,
                    "textQuery": 0.0,
                    "filters": 0.0,
                    "groupBy": 0.0,
                    "timeRange": 0.0
                  }
                }

                Confidence scores must be numbers from 0.0 to 1.0.

                If the request cannot be resolved, return:
                {
                  "intent": "UNRESOLVABLE",
                  "reason": "brief reason",
                  "textQuery": null,
                  "filters": {
                    "source": null,
                    "severity": null,
                    "event_type": null,
                    "action": null,
                    "user": null,
                    "host": null,
                    "ip": null,
                    "geo_location": null,
                    "user_agent": null
                  },
                  "groupBy": null,
                  "metric": null,
                  "topN": null,
                  "timeBucket": null,
                  "timeRange": {
                    "from": null,
                    "to": null
                  },
                  "recurringTime": null,
                  "contextUsage": "NOT_AVAILABLE",
                  "overrideIntent": null,
                  "overrideReason": null,
                  "semanticSpans": [],
                  "unresolvedAmbiguities": ["brief ambiguity"],
                  "confidenceScores": {
                    "intent": 0.0,
                    "textQuery": 0.0,
                    "filters": 0.0,
                    "groupBy": 0.0,
                    "timeRange": 0.0
                  }
                }

                === ANALYST QUESTION ===
                %s
                """.formatted(
                Instant.now().toString(),
                SocEventPromptSchema.allowedFields(),
                SocEventPromptSchema.filterableFieldBullets(),

                value(request.request().getFrom()),
                value(request.request().getTo()),
                value(request.request().getSeverity()),
                value(request.request().getEventType()),
                value(request.request().getUser()),
                value(request.request().getHost()),
                value(request.request().getIp()),

                SocEventPromptSchema.filterableFieldBullets(),
                SocEventPromptSchema.groupableFieldBullets(),

                request.routingHint() == null ? "null" : request.routingHint().templateType(),
                request.routingHint() == null ? "null" : request.routingHint().confidence(),
                request.routingHint() == null ? "null" : value(request.routingHint().reason()),
                request.routingHint() != null && request.routingHint().neutral(),
                request.routingHint() != null && request.routingHint().lowConfidencePerception(),

                request.heuristicHint() == null ? "null" : request.heuristicHint().templateType(),
                request.heuristicHint() == null ? "null" : request.heuristicHint().confidence(),
                request.heuristicHint() != null && request.heuristicHint().ambiguous(),
                request.heuristicHint() == null ? "null" : value(request.heuristicHint().reason()),

                request.semanticHint() == null ? "null" : request.semanticHint().templateType(),
                request.semanticHint() == null ? "null" : request.semanticHint().confidence(),
                request.semanticHint() != null && request.semanticHint().ambiguous(),
                request.semanticHint() == null ? "null" : value(request.semanticHint().reason()),

                previous,
                request.enrichments() == null ? "null" : request.enrichments().toString(),

                value(request.request().getQuestion())
        );
    }

    private String value(String value) {
        if (value == null || value.isBlank() || "null".equalsIgnoreCase(value.trim())) {
            return "null";
        }
        return value.trim();
    }
}
