package vdt.se.demo.adapter.out.semantic;

import org.springframework.stereotype.Component;
import vdt.se.demo.application.port.outboundPort.semantic.MitreEnrichmentPort;

import java.util.List;
import java.util.Locale;

@Component
public class LocalMitreEnrichmentAdapter implements MitreEnrichmentPort {
    @Override
    public List<String> enrich(String question) {
        String text = question == null ? "" : question.toLowerCase(Locale.ROOT);
        if (text.matches(".*\\bt\\d{4}(?:\\.\\d{3})?\\b.*")) {
            return List.of("Question contains a MITRE technique id; preserve it as a text query term.");
        }
        if (text.contains("credential") || text.contains("login") || text.contains("auth")) {
            return List.of("Authentication wording may map to event_type=auth.");
        }
        return List.of();
    }
}
