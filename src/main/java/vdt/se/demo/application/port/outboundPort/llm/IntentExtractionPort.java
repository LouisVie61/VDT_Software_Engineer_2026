package vdt.se.demo.application.port.outboundPort.llm;

import vdt.se.demo.application.dto.IntentExtractionRequest;
import vdt.se.demo.domain.model.ExtractedIntent;

public interface IntentExtractionPort {
    ExtractedIntent extract(IntentExtractionRequest request);
}

