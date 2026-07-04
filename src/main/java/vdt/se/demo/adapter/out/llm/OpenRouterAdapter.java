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
import vdt.se.demo.domain.exception.LlmRateLimitException;
import vdt.se.demo.domain.exception.LlmRetryableException;
import vdt.se.demo.domain.valueObjects.LlmProvider;

import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public final class OpenRouterAdapter implements LlmProviderPort {
    private final AppProperties properties;
    private final RestClient restClient;
    private final ObjectMapper mapper;

    public OpenRouterAdapter(AppProperties properties, RestClient restClient, ObjectMapper mapper) {
        this.properties = properties;
        this.restClient = restClient;
        this.mapper = mapper;
    }

    @Override public LlmProvider provider() { return LlmProvider.OPENROUTER; }
    @Override public String complete(String prompt) { return complete(null, prompt); }
    @Override public String complete(String systemPrompt, String userPrompt) { return request(systemPrompt, userPrompt, List.of()); }
    @Override public String completeWithTools(String systemPrompt, String userPrompt, List<JsonNode> tools) { return request(systemPrompt, userPrompt, tools); }

    private String request(String systemPrompt, String userPrompt, List<JsonNode> tools) {
        AppProperties.OpenRouter config = properties.getLlm().getOpenrouter();
        String apiKey = resolveApiKey(config);
        if (apiKey == null || apiKey.isBlank()) {
            throw new LlmRetryableException("OpenRouter API key is not configured (OPENROUTER_API_KEY or .claude/settings.json)");
        }
        try {
            String response = restClient.post().uri(config.getBaseUrl())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .header("HTTP-Referer", "http://localhost")
                    .header("X-Title", "VDT SOC Event Search")
                    .contentType(MediaType.APPLICATION_JSON).body(body(config, systemPrompt, userPrompt, tools))
                    .retrieve().body(String.class);
            return extract(response);
        } catch (LlmException e) {
            throw e;
        } catch (ResourceAccessException e) {
            throw new LlmRetryableException("OpenRouter request timed out or was unavailable", e);
        } catch (RestClientResponseException e) {
            String detail = "status=" + e.getStatusCode().value() + ", body=" + responseBody(e);
            if (e.getStatusCode().value() == 429) throw new LlmRateLimitException("OpenRouter rate limited the request: " + detail, e);
            if (isRetryable(e.getStatusCode())) throw new LlmRetryableException("OpenRouter retryable request failure: " + detail, e);
            throw new LlmException("OpenRouter request failed: " + detail, e);
        } catch (RuntimeException e) {
            throw new LlmException("OpenRouter response parsing failed", e);
        }
    }

    private String resolveApiKey(AppProperties.OpenRouter config) {
        if (config.getApiKey() != null && !config.getApiKey().isBlank()) return config.getApiKey();
        try {
            JsonNode root = mapper.readTree(Files.readString(config.getCredentialsFile()));
            String token = root.path("env").path("ANTHROPIC_AUTH_TOKEN").asString();
            return token.isBlank() ? null : token;
        } catch (Exception ignored) {
            return null;
        }
    }

    private Map<String, Object> body(AppProperties.OpenRouter config, String systemPrompt, String userPrompt, List<JsonNode> definitions) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", config.getModel());
        body.put("temperature", 0.1d);
        body.put("messages", systemPrompt == null || systemPrompt.isBlank()
                ? List.of(Map.of("role", "user", "content", userPrompt))
                : List.of(Map.of("role", "system", "content", systemPrompt), Map.of("role", "user", "content", userPrompt)));
        if (definitions == null || definitions.isEmpty()) {
            body.put("response_format", Map.of("type", "json_object"));
        } else {
            body.put("tools", definitions.stream().map(definition -> Map.of("type", "function", "function", Map.of(
                    "name", definition.path("name").asString(), "description", definition.path("description").asString(),
                    "parameters", definition.path("input_schema")))).toList());
            body.put("tool_choice", "required");
        }
        return body;
    }

    private String extract(String response) {
        JsonNode message = mapper.readTree(response).path("choices").path(0).path("message");
        JsonNode function = message.path("tool_calls").path(0).path("function");
        if (function.path("name").isString()) {
            return mapper.writeValueAsString(Map.of("name", function.path("name").asString(),
                    "arguments", mapper.readTree(function.path("arguments").asString())));
        }
        String content = message.path("content").asString();
        if (content.isBlank()) throw new LlmException("OpenRouter response contained neither a tool call nor text content");
        return content;
    }

    private boolean isRetryable(HttpStatusCode status) { return status.value() == 408 || status.value() == 409 || status.is5xxServerError(); }
    private String responseBody(RestClientResponseException e) {
        String body = e.getResponseBodyAsString();
        return body == null || body.isBlank() ? "<empty>" : body.substring(0, Math.min(body.length(), 1000));
    }
}
