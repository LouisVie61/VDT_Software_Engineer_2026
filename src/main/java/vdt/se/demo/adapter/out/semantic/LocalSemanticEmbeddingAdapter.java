package vdt.se.demo.adapter.out.semantic;

import org.springframework.stereotype.Component;
import vdt.se.demo.application.port.outboundPort.semantic.EmbeddingPort;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class LocalSemanticEmbeddingAdapter implements EmbeddingPort {
    @Override
    public double similarity(String left, String right) {
        Set<String> a = tokens(left);
        Set<String> b = tokens(right);
        if (a.isEmpty() || b.isEmpty()) {
            return 0.0d;
        }
        long intersection = a.stream().filter(b::contains).count();
        long union = java.util.stream.Stream.concat(a.stream(), b.stream()).distinct().count();
        return union == 0 ? 0.0d : (double) intersection / (double) union;
    }

    private Set<String> tokens(String value) {
        return Arrays.stream((value == null ? "" : value.toLowerCase(Locale.ROOT)).split("[^a-z0-9_]+"))
                .filter(token -> token.length() > 2)
                .collect(Collectors.toSet());
    }
}
