package vdt.se.demo.adapter.out.llm;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.adapter.config.AppProperties;
import vdt.se.demo.application.port.outboundPort.LlmProviderAdapter;
import vdt.se.demo.domain.exception.LlmRetryableException;
import vdt.se.demo.domain.valueObjects.LlmProvider;

import java.util.List;
import java.util.Map;

@Component
public class GroqAdapter implements LlmProviderAdapter {

    private final AppProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GroqAdapter(AppProperties properties, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.properties = properties;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public LlmProvider provider() {
        return LlmProvider.GROQ;
    }

    @Override
    public String complete(String prompt) {
        String apiKey = properties.getLlm().getGroq().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new LlmRetryableException("Groq API key is not configured");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            Map<String, Object> body = Map.of(
                    "model", properties.getLlm().getGroq().getModel(),
                    "temperature", 0,
                    "messages", List.of(Map.of("role", "user", "content", prompt))
            );
            String response = restTemplate.postForObject(
                    properties.getLlm().getGroq().getBaseUrl(),
                    new HttpEntity<>(body, headers),
                    String.class
            );
            JsonNode root = objectMapper.readTree(response);
            return root.get("choices").get(0).get("message").get("content").asString();
        } catch (Exception e) {
            throw new LlmRetryableException("Groq request failed", e);
        }
    }
}
