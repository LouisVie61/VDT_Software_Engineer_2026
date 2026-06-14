package vdt.se.demo.adapter.out.llm;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import org.springframework.stereotype.Component;
import vdt.se.demo.adapter.config.AppProperties;
import vdt.se.demo.application.port.outboundPort.LlmProviderPort;
import vdt.se.demo.domain.exception.LlmRetryableException;
import vdt.se.demo.domain.valueObjects.LlmProvider;

@Component
public class GeminiAdapter implements LlmProviderPort {

    private final AppProperties properties;

    public GeminiAdapter(AppProperties properties) {
        this.properties = properties;
    }

    @Override
    public LlmProvider provider() {
        return LlmProvider.GEMINI;
    }

    @Override
    public String complete(String prompt) {
        String apiKey = properties.getLlm().getGemini().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new LlmRetryableException("Gemini API key is not configured");
        }
        try {
            Client client = Client.builder().apiKey(apiKey).build();
            GenerateContentResponse response = client.models.generateContent(
                    properties.getLlm().getGemini().getModel(),
                    prompt,
                    null
            );
            return response.text();
        } catch (Exception e) {
            throw new LlmRetryableException("Gemini request failed", e);
        }
    }
}
