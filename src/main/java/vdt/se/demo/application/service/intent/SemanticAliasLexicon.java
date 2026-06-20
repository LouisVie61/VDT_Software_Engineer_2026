package vdt.se.demo.application.service.intent;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class SemanticAliasLexicon {
    private final Map<Kind, Map<String, Set<String>>> aliases = aliases();

    public Optional<ResolvedAlias> resolve(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        String normalized = token.toLowerCase(Locale.ROOT);
        Optional<ResolvedAlias> exact = exact(normalized);
        return exact.isPresent() ? exact : fuzzy(normalized);
    }

    public boolean containsKind(String text, Kind kind) {
        if (text == null || text.isBlank()) {
            return false;
        }
        for (String token : text.split("\\s+")) {
            Optional<ResolvedAlias> resolved = resolve(token);
            if (resolved.isPresent() && resolved.get().kind() == kind) {
                return true;
            }
        }
        return false;
    }

    private Optional<ResolvedAlias> exact(String token) {
        for (Map.Entry<Kind, Map<String, Set<String>>> byKind : aliases.entrySet()) {
            for (Map.Entry<String, Set<String>> byCanonical : byKind.getValue().entrySet()) {
                if (byCanonical.getValue().contains(token)) {
                    return Optional.of(new ResolvedAlias(byKind.getKey(), byCanonical.getKey()));
                }
            }
        }
        return Optional.empty();
    }

    private Optional<ResolvedAlias> fuzzy(String token) {
        if (token.length() < 4) {
            return Optional.empty();
        }
        ResolvedAlias best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (Map.Entry<Kind, Map<String, Set<String>>> byKind : aliases.entrySet()) {
            for (Map.Entry<String, Set<String>> byCanonical : byKind.getValue().entrySet()) {
                for (String alias : byCanonical.getValue()) {
                    if (alias.length() < 4 || Math.abs(alias.length() - token.length()) > 2) {
                        continue;
                    }
                    int distance = levenshtein(token, alias);
                    if (distance <= tolerance(alias) && distance < bestDistance) {
                        best = new ResolvedAlias(byKind.getKey(), byCanonical.getKey());
                        bestDistance = distance;
                    }
                }
            }
        }
        return Optional.ofNullable(best);
    }

    private int tolerance(String alias) {
        return alias.length() >= 7 ? 2 : 1;
    }

    private int levenshtein(String left, String right) {
        int[] previous = new int[right.length() + 1];
        int[] current = new int[right.length() + 1];
        for (int j = 0; j <= right.length(); j++) {
            previous[j] = j;
        }
        for (int i = 1; i <= left.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= right.length(); j++) {
                int cost = left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(Math.min(current[j - 1] + 1, previous[j] + 1), previous[j - 1] + cost);
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[right.length()];
    }

    private Map<Kind, Map<String, Set<String>>> aliases() {
        Map<Kind, Map<String, Set<String>>> values = new LinkedHashMap<>();
        values.put(Kind.ACTION, Map.of(
                "success", Set.of("success", "successful", "succeeded", "sucess", "succes", "succ"),
                "failed", Set.of("fail", "failed", "failure", "unsuccessful", "faild", "faill", "fl"),
                "locked", Set.of("lock", "locked", "lockout", "lockd"),
                "bypass", Set.of("bypass", "bypas")
        ));
        values.put(Kind.AUTH, Map.of(
                "auth", Set.of("auth", "authentication", "login", "logon", "signin", "sign-in", "logn", "lgoin")
        ));
        values.put(Kind.SEVERITY, Map.of(
                "critical", Set.of("critical", "crit", "critcal", "critial"),
                "high", Set.of("high", "hi"),
                "medium", Set.of("medium", "med", "medum"),
                "low", Set.of("low"),
                "info", Set.of("info", "information", "informational")
        ));
        values.put(Kind.FIELD, Map.of(
                "event_type", Set.of("event_type", "eventtype", "type", "category", "loai"),
                "severity", Set.of("severity", "level", "mucdo"),
                "action", Set.of("action", "outcome", "status"),
                "user", Set.of("user", "username", "account", "usr"),
                "host", Set.of("host", "machine", "endpoint", "device"),
                "ip", Set.of("ip", "srcip", "dstip"),
                "source", Set.of("source", "src", "sensor")
        ));
        values.put(Kind.COMMAND, Map.of(
                "command", Set.of("show", "list", "find", "get", "search", "statistic", "statistics", "stats",
                        "stat", "ss", "analyze", "analyse", "top", "count", "group", "by", "theo", "tung", "dem",
                        "thong", "ke", "nhieu", "nhat", "co", "trong", "dau", "la")
        ));
        values.put(Kind.TARGET, Map.of(
                "target", Set.of("event", "events", "alert", "alerts", "log", "logs", "record", "records",
                        "result", "results", "data", "su", "kien", "canh", "bao", "ket", "qua", "du", "lieu",
                        "loi", "error", "errors")
        ));
        values.put(Kind.DATE, Map.of(
                "date", Set.of("today", "yesterday", "tomorrow", "current", "recent", "latest", "last", "past",
                        "previous", "this", "month", "week", "day", "days", "hour", "hours", "year", "years",
                        "nam", "ngay", "gio", "hom", "nay", "qua", "tu", "den")
        ));
        return values;
    }

    public enum Kind {
        ACTION,
        AUTH,
        SEVERITY,
        FIELD,
        COMMAND,
        TARGET,
        DATE
    }

    public record ResolvedAlias(Kind kind, String canonical) {
    }
}
