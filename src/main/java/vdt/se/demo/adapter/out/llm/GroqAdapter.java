package vdt.se.demo.adapter.out.llm;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
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
import vdt.se.demo.domain.exception.LlmRateLimitException;
import vdt.se.demo.domain.valueObjects.LlmProvider;

import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class GroqAdapter implements LlmProviderPort {

    private final AppProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public GroqAdapter(AppProperties properties, RestClient restClient, ObjectMapper objectMapper) {
        this.properties = properties;
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public LlmProvider provider() {
        return LlmProvider.GROQ;
    }

    @Override
    public String complete(String prompt) {
        return complete(null, prompt);
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        return completeRequest(systemPrompt, userPrompt, List.of());
    }

    @Override
    public String completeWithTools(String systemPrompt, String userPrompt, List<JsonNode> tools) {
        return completeRequest(systemPrompt, userPrompt, tools);
    }

    private String completeRequest(String systemPrompt, String userPrompt, List<JsonNode> tools) {
        String apiKey = properties.getLlm().getGroq().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new LlmRetryableException("Groq API key is not configured");
        }
        try {
            String response = restClient.post()
                    .uri(properties.getLlm().getGroq().getBaseUrl())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body(systemPrompt, userPrompt, tools))
                    .retrieve()
                    .onStatus(this::retryableStatus, (request, clientResponse) -> {
                        if (clientResponse.getStatusCode().value() == 429)
                            throw new LlmRateLimitException("Groq rate limited the request");
                        throw new LlmRetryableException("Groq retryable status: " + clientResponse.getStatusCode());
                    })
                    .body(String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode message = root.path("choices").path(0).path("message");
            JsonNode function = message.path("tool_calls").path(0).path("function");
            if (!function.isMissingNode() && function.path("name").isString()) {
                JsonNode arguments = objectMapper.readTree(function.path("arguments").asString());
                return objectMapper.writeValueAsString(Map.of("name", function.path("name").asString(), "arguments", arguments));
            }
            JsonNode text = message.path("content");
            if (text.isMissingNode() || text.asString().isBlank()) {
                throw new LlmException("Groq response did not contain text content");
            }
            return text.asString();
        } catch (LlmException e) {
            throw e;
        } catch (ResourceAccessException e) {
            throw new LlmRetryableException("Groq request timed out or was unavailable", e);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 429)
                throw new LlmRateLimitException("Groq rate limited the request", e);
            if (isRetryableStatus(e.getStatusCode())) {
                throw new LlmRetryableException("Groq retryable status: " + e.getStatusCode(), e);
            }
            throw new LlmException("Groq request failed with non-retryable status: " + e.getStatusCode()
                    + ", body=" + responseBody(e), e);
        } catch (Exception e) {
            if (isTimeout(e)) {
                throw new LlmRetryableException("Groq request timed out or was unavailable", e);
            }
            throw new LlmException("Groq response parsing failed", e);
        }
    }

    private Map<String, Object> body(String systemPrompt, String userPrompt, List<JsonNode> definitions) {
        Map<String,Object> body = new java.util.LinkedHashMap<>();
        body.put("model", properties.getLlm().getGroq().getModel());
        body.put("temperature", 0.1d);
        body.put("messages", messages(systemPrompt, userPrompt));
        if (definitions == null || definitions.isEmpty()) body.put("response_format", Map.of("type", "json_object"));
        else {
            body.put("tools", definitions.stream().map(definition -> Map.of("type", "function", "function", Map.of(
                    "name", definition.path("name").asString(),
                    "description", definition.path("description").asString(),
                    "parameters", definition.path("input_schema")))).toList());
            body.put("tool_choice", "required");
        }
        return body;
    }

    private List<Map<String, String>> messages(String systemPrompt, String userPrompt) {
        if (systemPrompt == null || systemPrompt.isBlank()) {
            return List.of(Map.of("role", "user", "content", userPrompt));
        }
        return List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        );
    }

    private boolean retryableStatus(HttpStatusCode status) {
        return isRetryableStatus(status);
    }

    private boolean isRetryableStatus(HttpStatusCode status) {
        return status.value() == 429 || status.value() == 503;
    }

    private boolean isTimeout(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SocketTimeoutException || current instanceof InterruptedIOException) {
                return true;
            }
            String message = current.getMessage();
            if (message != null && message.toLowerCase(Locale.ROOT).contains("timed out")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
    private String responseBody(RestClientResponseException exception) {
        String body = exception.getResponseBodyAsString();
        return body == null || body.isBlank() ? "<empty>" : body.substring(0, Math.min(body.length(), 1000));
    }
}
