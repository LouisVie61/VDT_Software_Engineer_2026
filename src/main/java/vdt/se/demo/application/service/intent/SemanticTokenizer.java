package vdt.se.demo.application.service.intent;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SemanticTokenizer {
    private static final Pattern TOKEN = Pattern.compile("\\b[a-z0-9._:/-]+\\b");

    public TokenizedQuery tokenize(String query) {
        String normalized = canonicalize(query);
        List<SemanticToken> tokens = new ArrayList<>();
        Matcher matcher = TOKEN.matcher(normalized);
        while (matcher.find()) {
            tokens.add(new SemanticToken(matcher.group(), matcher.start(), matcher.end()));
        }
        return new TokenizedQuery(normalized, tokens);
    }

    private String canonicalize(String value) {
        String asciiD = (value == null ? "" : value).replace('\u0111', 'd').replace('\u0110', 'D');
        String normalized = Normalizer.normalize(asciiD, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
    }

    public record TokenizedQuery(String normalizedQuery, List<SemanticToken> tokens) {
    }
}
