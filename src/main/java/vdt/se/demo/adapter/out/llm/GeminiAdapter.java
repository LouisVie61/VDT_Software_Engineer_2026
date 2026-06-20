package vdt.se.demo.adapter.out.llm;

import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.adapter.config.AppProperties;
import vdt.se.demo.application.port.outboundPort.llm.LlmProviderPort;
import vdt.se.demo.domain.exception.LlmException;
import vdt.se.demo.domain.exception.LlmRetryableException;
import vdt.se.demo.domain.valueObjects.LlmProvider;

import java.util.List;
import java.util.Map;

@Component
public class GeminiAdapter implements LlmProviderPort {

    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";

    private final AppProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public GeminiAdapter(AppProperties properties, RestClient restClient, ObjectMapper objectMapper) {
        this.properties = properties;
        this.restClient = restClient;
        this.objectMapper = objectMapper;
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
            String response = restClient.post()
                    .uri(GEMINI_BASE_URL + properties.getLlm().getGemini().getModel()
                            + ":generateContent?key=" + apiKey)
                    .body(body(prompt))
                    .retrieve()
                    .onStatus(this::retryableStatus, (request, clientResponse) -> {
                        throw new LlmRetryableException("Gemini retryable status: " + clientResponse.getStatusCode());
                    })
                    .body(String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode text = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
            if (text.isMissingNode() || text.asString().isBlank()) {
                throw new LlmException("Gemini response did not contain text content");
            }
            return text.asString();
        } catch (LlmException e) {
            throw e;
        } catch (ResourceAccessException e) {
            throw new LlmRetryableException("Gemini request timed out or was unavailable", e);
        } catch (RestClientResponseException e) {
            if (isRetryableStatus(e.getStatusCode())) {
                throw new LlmRetryableException("Gemini retryable status: " + e.getStatusCode(), e);
            }
            throw new LlmException("Gemini request failed with non-retryable status: " + e.getStatusCode(), e);
        } catch (Exception e) {
            throw new LlmException("Gemini response parsing failed", e);
        }
    }

    private Map<String, Object> body(String prompt) {
        return Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "temperature", 0.1d
                )
        );
    }

    private boolean retryableStatus(HttpStatusCode status) {
        return isRetryableStatus(status);
    }

    private boolean isRetryableStatus(HttpStatusCode status) {
        return status.value() == 429 || status.value() == 503;
    }
}
