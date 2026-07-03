package vdt.se.demo.application.service.intent;

import vdt.se.demo.application.dto.SearchRequest;
import vdt.se.demo.domain.model.RecurringTime;
import vdt.se.demo.domain.model.SearchIntent;
import vdt.se.demo.domain.model.SemanticSpan;
import vdt.se.demo.domain.valueObjects.TemplateType;

import java.text.Normalizer;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchIntentNormalizer {
    private static final Pattern VI_MONTH_PATTERN = Pattern.compile("\\bthang\\s+(1[0-2]|[1-9])\\b");
    private static final Map<String, Integer> EN_MONTHS = Map.ofEntries(
            Map.entry("january", 1),
            Map.entry("february", 2),
            Map.entry("march", 3),
            Map.entry("april", 4),
            Map.entry("may", 5),
            Map.entry("june", 6),
            Map.entry("july", 7),
            Map.entry("august", 8),
            Map.entry("september", 9),
            Map.entry("october", 10),
            Map.entry("november", 11),
            Map.entry("december", 12)
    );

    private final SearchTimeExpressionResolver timeExpressionResolver;
    private final SearchFilterValidator filterValidator;

    public SearchIntentNormalizer() {
        this(new SearchTimeExpressionResolver(), new SearchFilterValidator());
    }

    public SearchIntentNormalizer(SearchTimeExpressionResolver timeExpressionResolver) {
        this(timeExpressionResolver, new SearchFilterValidator());
    }

    public SearchIntentNormalizer(SearchTimeExpressionResolver timeExpressionResolver,
                                  SearchFilterValidator filterValidator) {
        this.timeExpressionResolver = timeExpressionResolver;
        this.filterValidator = filterValidator;
    }

    public SearchIntent normalize(SearchRequest request, SearchIntent input) {
        SearchIntent intent = input == null ? new SearchIntent() : input.copy();
        intent.setFilters(normalizedFilters(intent.getFilters()));
        filterValidator.validateAndNormalize(intent);
        removeFiltersOverriddenByRequest(request, intent);

        Optional<Integer> annualMonth = annualRecurringMonth(request == null ? null : request.getQuestion(), intent);
        if (annualMonth.isPresent()) {
            intent.setTimeFrom(null);
            intent.setTimeTo(null);
            intent.setTimeBucket(null);
            intent.setRecurringTime(RecurringTime.builder()
                    .mode("EVERY_YEAR")
                    .month(annualMonth.get())
                    .build());
            intent.setSemanticSpans(withAnnualRecurrenceWarning(intent.getSemanticSpans(), request.getQuestion(), annualMonth.get()));
        }
        timeExpressionResolver.apply(request, intent);
        intent.setTextQuery(cleanTextQuery(intent.getTextQuery()));
        intent.setGroupBy(cleanField(intent.getGroupBy()));
        intent.setTimeBucket(cleanText(intent.getTimeBucket()));
        if ("quarter".equalsIgnoreCase(intent.getTimeBucket()) && !asksForQuarter(request.getQuestion())) {
            intent.setTimeBucket(null);
        }

        if (intent.getIntent() == null) {
            intent.setIntent(TemplateType.SIMPLE_SEARCH);
        }
        if (intent.getIntent() == TemplateType.TIME_AGGREGATION && !hasText(intent.getTimeBucket())) {
            boolean broadRange = hasText(intent.getTimeFrom());
            intent.setTimeBucket(timeExpressionResolver.inferBucket(intent.getSemanticSpans(), broadRange));
        }
        return intent;
    }

    private Map<String, String> normalizedFilters(Map<String, String> rawFilters) {
        Map<String, String> filters = new LinkedHashMap<>();
        if (rawFilters == null) {
            return filters;
        }
        rawFilters.forEach((field, value) -> {
            String normalizedField = canonicalize(field).replace(' ', '_');
            if (hasText(value)) {
                filters.put(normalizedField, value.trim());
            }
        });
        return filters;
    }

    private void removeFiltersOverriddenByRequest(SearchRequest request, SearchIntent intent) {
        removeIfRequestHasValue(intent, "severity", request.getSeverity());
        removeIfRequestHasValue(intent, "event_type", request.getEventType());
        removeIfRequestHasValue(intent, "user", request.getUser());
        removeIfRequestHasValue(intent, "host", request.getHost());
        removeIfRequestHasValue(intent, "ip", request.getIp());
    }

    private void removeIfRequestHasValue(SearchIntent intent, String field, String value) {
        if (hasText(value)) {
            intent.getFilters().remove(field);
        }
    }

    private String canonicalize(String value) {
        String asciiD = value(value).replace('\u0111', 'd').replace('\u0110', 'D');
        String normalized = Normalizer.normalize(asciiD, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}+", "").toLowerCase(java.util.Locale.ROOT);
    }

    private String cleanField(String value) {
        if (!hasText(value)) {
            return null;
        }
        return canonicalize(value).replace(' ', '_');
    }

    private String cleanText(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private String cleanTextQuery(String value) {
        String cleaned = cleanText(value);
        if (cleaned == null) {
            return null;
        }
        String normalized = canonicalize(cleaned);
        if ("loi".equals(normalized) || "error".equals(normalized) || "errors".equals(normalized)) {
            return null;
        }
        return cleaned;
    }

    private boolean asksForQuarter(String question) {
        String normalized = canonicalize(question);
        return normalized.contains("theo quy")
                || normalized.contains("moi quy")
                || normalized.contains("by quarter")
                || normalized.contains("per quarter")
                || normalized.contains("quarterly");
    }

    private Optional<Integer> annualRecurringMonth(String question, SearchIntent intent) {
        String normalized = canonicalize(question);
        boolean annual = (normalized.contains("hang nam")
                || normalized.contains("moi nam")
                || normalized.contains("yearly")
                || normalized.contains("annually")
                || normalized.contains("every year"));
        if (!annual) {
            return Optional.empty();
        }
        return monthFromQuestion(normalized).or(() -> monthFromTimeFrom(intent == null ? null : intent.getTimeFrom()));
    }

    private Optional<Integer> monthFromQuestion(String normalizedQuestion) {
        Matcher matcher = VI_MONTH_PATTERN.matcher(normalizedQuestion);
        if (matcher.find()) {
            return Optional.of(Integer.parseInt(matcher.group(1)));
        }
        for (Map.Entry<String, Integer> entry : EN_MONTHS.entrySet()) {
            if (normalizedQuestion.contains(entry.getKey())) {
                return Optional.of(entry.getValue());
            }
        }
        return Optional.empty();
    }

    private Optional<Integer> monthFromTimeFrom(String timeFrom) {
        if (!hasText(timeFrom)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Instant.parse(timeFrom.trim()).atZone(java.time.ZoneOffset.UTC).getMonthValue());
        } catch (DateTimeParseException ignored) {
            return Optional.empty();
        }
    }

    private List<SemanticSpan> withAnnualRecurrenceWarning(List<SemanticSpan> spans, String question, int month) {
        List<SemanticSpan> next = new java.util.ArrayList<>(spans == null ? List.of() : spans);
        next.add(SemanticSpan.builder()
                .kind(SemanticSpan.Kind.TEMPORAL)
                .status(SemanticSpan.Status.UNSUPPORTED)
                .text(question == null ? "annual recurring month" : question)
                .canonical("annual_recurring_month:%d".formatted(month))
                .start(0)
                .end(question == null ? 0 : question.length())
                .build());
        return next;
    }

    private String value(String value) {
        return value == null ? "" : value;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
