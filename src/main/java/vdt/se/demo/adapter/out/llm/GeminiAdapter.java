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
import vdt.se.demo.domain.exception.LlmRateLimitException;
import vdt.se.demo.domain.valueObjects.LlmProvider;

import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.util.LinkedHashMap;
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
        return complete(null, prompt);
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        return completeRequest(systemPrompt, userPrompt, List.of());
    }

    @Override
    public String completeWithTools(String systemPrompt, String userPrompt, List<JsonNode> tools) {
        return completeRequest(systemPrompt, userPrompt, tools, queryConfig());
    }

    @Override
    public String completeWithTools(String systemPrompt, String userPrompt, List<JsonNode> tools,
                                    String queryText, boolean forceComplexModel) {
        AppProperties.Gemini config = queryConfig();
        boolean complex = forceComplexModel || (queryText != null
                && queryText.codePointCount(0, queryText.length()) > config.getComplexQueryLength());
        String model = complex ? config.getComplexModel() : config.getModel();
        return completeRequest(systemPrompt, userPrompt, tools, config, model);
    }

    public String completeSummary(String systemPrompt, String userPrompt) {
        return completeRequest(systemPrompt, userPrompt, null, properties.getLlm().getGemini1());
    }

    private String completeRequest(String systemPrompt, String userPrompt, List<JsonNode> tools) {
        return completeRequest(systemPrompt, userPrompt, tools, queryConfig());
    }

    private String completeRequest(String systemPrompt, String userPrompt, List<JsonNode> tools,
                                   AppProperties.Gemini config) {
        return completeRequest(systemPrompt, userPrompt, tools, config, config.getModel());
    }

    private AppProperties.Gemini queryConfig() {
        AppProperties.Gemini gemini2 = properties.getLlm().getGemini2();
        return gemini2.getApiKey() == null || gemini2.getApiKey().isBlank()
                ? properties.getLlm().getGemini()
                : gemini2;
    }

    private String completeRequest(String systemPrompt, String userPrompt, List<JsonNode> tools,
                                   AppProperties.Gemini config, String model) {
        String apiKey = config.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new LlmRetryableException("Gemini API key is not configured");
        }
        try {
            String response = restClient.post()
                    .uri(GEMINI_BASE_URL + model
                            + ":generateContent?key=" + apiKey)
                    .body(body(systemPrompt, userPrompt, tools))
                    .retrieve()
                    .onStatus(this::retryableStatus, (request, clientResponse) -> {
                        if (clientResponse.getStatusCode().value() == 429)
                            throw new LlmRateLimitException("Gemini rate limited the request");
                        throw new LlmRetryableException("Gemini retryable status: " + clientResponse.getStatusCode());
                    })
                    .body(String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode part = root.path("candidates").path(0).path("content").path("parts").path(0);
            JsonNode function = part.path("functionCall");
            if (!function.isMissingNode() && function.path("name").isString()) {
                return objectMapper.writeValueAsString(Map.of("name", function.path("name").asString(), "arguments", function.path("args")));
            }
            JsonNode text = part.path("text");
            if (text.isMissingNode() || text.asString().isBlank()) {
                throw new LlmException("Gemini response did not contain text content");
            }
            return text.asString();
        } catch (LlmException e) {
            throw e;
        } catch (ResourceAccessException e) {
            throw new LlmRetryableException("Gemini request timed out or was unavailable", e);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 429)
                throw new LlmRateLimitException("Gemini rate limited the request", e);
            if (isRetryableStatus(e.getStatusCode())) {
                throw new LlmRetryableException("Gemini retryable status: " + e.getStatusCode(), e);
            }
            throw new LlmException("Gemini request failed with non-retryable status: " + e.getStatusCode()
                    + ", body=" + responseBody(e), e);
        } catch (Exception e) {
            if (isTimeout(e)) {
                throw new LlmRetryableException("Gemini request timed out or was unavailable", e);
            }
            throw new LlmException("Gemini response parsing failed", e);
        }
    }

    private Map<String, Object> body(String systemPrompt, String userPrompt, List<JsonNode> definitions) {
        Map<String, Object> body = new LinkedHashMap<>();

        if (systemPrompt != null && !systemPrompt.isBlank()) {
            body.put("systemInstruction", Map.of(
                    "parts", List.of(Map.of("text", systemPrompt))
            ));
        }

        body.put("contents", List.of(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", userPrompt))
        )));

        boolean hasTools = definitions != null && !definitions.isEmpty();

        if (hasTools) {
            body.put("generationConfig", Map.of(
                    "temperature", 0.1d
            ));

            body.put("tools", List.of(Map.of(
                    "functionDeclarations",
                    definitions.stream()
                            .map(definition -> Map.of(
                                    "name", definition.path("name").asString(),
                                    "description", definition.path("description").asString(),
                                    "parameters", definition.path("input_schema")
                            ))
                            .toList()
            )));

            body.put("toolConfig", Map.of(
                    "functionCallingConfig",
                    Map.of("mode", "ANY")
            ));
        } else if (definitions != null) {
            body.put("generationConfig", Map.of(
                    "responseMimeType", "application/json",
                    "temperature", 0.1d
            ));
        } else {
            body.put("generationConfig", Map.of("temperature", 0.1d));
        }

        return body;
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
            if (message != null && message.toLowerCase(java.util.Locale.ROOT).contains("timed out")) {
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
