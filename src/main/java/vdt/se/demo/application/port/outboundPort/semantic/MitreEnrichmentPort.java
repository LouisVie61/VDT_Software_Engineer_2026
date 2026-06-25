package vdt.se.demo.application.port.outboundPort.semantic;

import java.util.List;

public interface MitreEnrichmentPort {
    List<String> enrich(String question);
}

