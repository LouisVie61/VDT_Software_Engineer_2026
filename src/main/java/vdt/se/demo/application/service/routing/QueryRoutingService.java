package vdt.se.demo.application.service.routing;

import vdt.se.demo.application.port.outboundPort.semantic.EmbeddingPort;
import vdt.se.demo.domain.model.RoutingHint;
import vdt.se.demo.domain.valueObjects.TemplateType;

import java.text.Normalizer;
import java.util.regex.Pattern;

public class QueryRoutingService {
    private static final Pattern YEAR_PATTERN = Pattern.compile("\\b(?:19|20)\\d{2}\\b");

    private final EmbeddingPort embeddingPort;

    public QueryRoutingService(EmbeddingPort embeddingPort) {
        this.embeddingPort = embeddingPort;
    }

    public RoutingDecision route(String question) {
        RoutingHint heuristic = heuristic(question);
        RoutingHint semantic = semantic(question);
        return new RoutingDecision(heuristic, semantic);
    }

    private RoutingHint heuristic(String question) {
        String text = normalize(question);
        if (containsAny(text, "nhieu nhat", "most common", "top error", "top loi", "dau la loi")) {
            return RoutingHint.builder()
                    .templateType(TemplateType.TERMS_AGGREGATION)
                    .confidence(0.76d)
                    .reason("top/error keyword")
                    .semantic(false)
                    .build();
        }
        if (containsStatisticsWord(text) && containsAny(text, "loi", "error", "errors", "failure", "failures")) {
            return RoutingHint.builder()
                    .templateType(TemplateType.TERMS_AGGREGATION)
                    .confidence(0.78d)
                    .reason("statistics/error keyword")
                    .semantic(false)
                    .build();
        }
        if (containsAny(text, "over time", "timeline", "theo thoi gian", "so event theo gio",
                "hourly", "daily") || (containsStatisticsWord(text) && containsTimeExpression(text))) {
            return RoutingHint.builder()
                    .templateType(TemplateType.TIME_AGGREGATION)
                    .confidence(0.82d)
                    .reason("time/statistics keyword")
                    .semantic(false)
                    .build();
        }
        if (containsAny(text, "top", "group by", "group", "by ", "theo tung", "dem", "count",
                "statistics by", "stats by", "thong ke theo")) {
            return RoutingHint.builder()
                    .templateType(TemplateType.TERMS_AGGREGATION)
                    .confidence(0.74d)
                    .reason("aggregation keyword")
                    .semantic(false)
                    .build();
        }
        if (text.length() < 12) {
            return RoutingHint.builder()
                    .templateType(TemplateType.SIMPLE_SEARCH)
                    .confidence(0.30d)
                    .reason("short text")
                    .semantic(false)
                    .build();
        }
        return RoutingHint.builder()
                .templateType(TemplateType.SIMPLE_SEARCH)
                .confidence(0.68d)
                .reason("default search")
                .semantic(false)
                .build();
    }

    private RoutingHint semantic(String question) {
        String text = question == null ? "" : question;
        double terms = Math.max(
                embeddingPort.similarity(text, "top count grouped by field"),
                embeddingPort.similarity(text, "statistics by user host ip severity event type")
        );
        double time = embeddingPort.similarity(text, "events over time histogram timeline");
        if (time > terms && time > 0.25d) {
            return RoutingHint.builder()
                    .templateType(TemplateType.TIME_AGGREGATION)
                    .confidence(Math.min(0.69d, time))
                    .reason("semantic time match")
                    .semantic(true)
                    .build();
        }
        if (terms > 0.25d) {
            return RoutingHint.builder()
                    .templateType(TemplateType.TERMS_AGGREGATION)
                    .confidence(Math.min(0.69d, terms))
                    .reason("semantic terms match")
                    .semantic(true)
                    .build();
        }
        return RoutingHint.builder()
                .templateType(TemplateType.SIMPLE_SEARCH)
                .confidence(0.30d)
                .reason("semantic low confidence")
                .semantic(true)
                .build();
    }

    private boolean containsStatisticsWord(String text) {
        return containsAny(text, "statistic", "statistics", "stats", "thong ke", "dem", "count");
    }

    private boolean containsTimeExpression(String text) {
        return YEAR_PATTERN.matcher(text).find()
                || containsAny(text, "last ", "past ", "previous ", "ngay qua", "gio qua", "nam ", "year ");
    }

    private boolean containsAny(String text, String... values) {
        for (String value : values) {
            if (text.contains(value)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String question) {
        String normalized = Normalizer.normalize(question == null ? "" : question, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}+", "").toLowerCase(java.util.Locale.ROOT);
    }

    public record RoutingDecision(RoutingHint heuristic, RoutingHint semantic) {
        public boolean bothLowConfidence() {
            return heuristic != null && semantic != null && heuristic.lowConfidence() && semantic.lowConfidence();
        }

        public RoutingHint strongestSignal() {
            if (semantic == null) {
                return heuristic;
            }
            if (heuristic == null) {
                return semantic;
            }
            return semantic.confidence() > heuristic.confidence() ? semantic : heuristic;
        }
    }
}
