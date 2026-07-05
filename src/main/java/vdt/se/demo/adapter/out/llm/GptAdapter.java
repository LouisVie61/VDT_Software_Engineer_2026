// package vdt.se.demo.adapter.out.llm;

// import org.springframework.http.HttpHeaders;
// import org.springframework.http.HttpStatusCode;
// import org.springframework.http.MediaType;
// import org.springframework.stereotype.Component;
// import org.springframework.web.client.ResourceAccessException;
// import org.springframework.web.client.RestClient;
// import org.springframework.web.client.RestClientResponseException;
// import tools.jackson.databind.JsonNode;
// import tools.jackson.databind.ObjectMapper;
// import vdt.se.demo.adapter.config.AppProperties;
// import vdt.se.demo.application.port.outboundPort.llm.LlmProviderPort;
// import vdt.se.demo.domain.exception.LlmException;
// import vdt.se.demo.domain.exception.LlmRateLimitException;
// import vdt.se.demo.domain.exception.LlmRetryableException;
// import vdt.se.demo.domain.valueObjects.LlmProvider;

// import java.io.InterruptedIOException;
// import java.net.SocketTimeoutException;
// import java.util.LinkedHashMap;
// import java.util.List;
// import java.util.Locale;
// import java.util.Map;

// @Component
// public class GptAdapter implements LlmProviderPort {
//     private final AppProperties properties;
//     private final RestClient restClient;
//     private final ObjectMapper mapper;

//     public GptAdapter(AppProperties properties, RestClient restClient, ObjectMapper mapper) {
//         this.properties = properties;
//         this.restClient = restClient;
//         this.mapper = mapper;
//     }

//     @Override
//     public LlmProvider provider() { return LlmProvider.GPT; }

//     @Override
//     public String complete(String prompt) { return complete(null, prompt); }

//     @Override
//     public String complete(String systemPrompt, String userPrompt) {
//         return completeRequest(systemPrompt, userPrompt, List.of());
//     }

//     @Override
//     public String completeWithTools(String systemPrompt, String userPrompt, List<JsonNode> tools) {
//         return completeRequest(systemPrompt, userPrompt, tools);
//     }

//     private String completeRequest(String systemPrompt, String userPrompt, List<JsonNode> tools) {
//         AppProperties.Gpt config = properties.getLlm().getGpt();
//         if (config.getApiKey() == null || config.getApiKey().isBlank()) {
//             throw new LlmRetryableException("OpenAI API key is not configured (OPENAI_API_KEY)");
//         }
//         try {
//             String response = restClient.post()
//                     .uri(config.getBaseUrl())
//                     .header(HttpHeaders.AUTHORIZATION, "Bearer " + config.getApiKey())
//                     .contentType(MediaType.APPLICATION_JSON)
//                     .body(body(systemPrompt, userPrompt, tools))
//                     .retrieve()
//                     .body(String.class);
//             return extract(response);
//         } catch (LlmException e) {
//             throw e;
//         } catch (ResourceAccessException e) {
//             throw new LlmRetryableException("OpenAI request timed out or was unavailable", e);
//         } catch (RestClientResponseException e) {
//             String detail = "status=" + e.getStatusCode().value() + ", body=" + responseBody(e);
//             if (e.getStatusCode().value() == 429) {
//                 throw new LlmRateLimitException("OpenAI rate limited the request: " + detail, e);
//             }
//             if (isRetryableStatus(e.getStatusCode())) {
//                 throw new LlmRetryableException("OpenAI retryable request failure: " + detail, e);
//             }
//             throw new LlmException("OpenAI request failed: " + detail, e);
//         } catch (RuntimeException e) {
//             if (isTimeout(e)) throw new LlmRetryableException("OpenAI request timed out or was unavailable", e);
//             throw new LlmException("OpenAI response parsing failed: " + rootMessage(e), e);
//         }
//     }

//     private String extract(String response) {
//         JsonNode root = mapper.readTree(response);
//         JsonNode message = root.path("choices").path(0).path("message");
//         JsonNode function = message.path("tool_calls").path(0).path("function");
//         if (function.path("name").isString()) {
//             String encodedArguments = function.path("arguments").asString();
//             JsonNode arguments = mapper.readTree(encodedArguments);
//             return mapper.writeValueAsString(Map.of(
//                     "name", function.path("name").asString(),
//                     "arguments", arguments));
//         }
//         JsonNode content = message.path("content");
//         if (!content.isString() || content.asString().isBlank()) {
//             throw new LlmException("OpenAI response contained neither a tool call nor text content");
//         }
//         return content.asString();
//     }

//     private Map<String, Object> body(String systemPrompt, String userPrompt, List<JsonNode> definitions) {
//         Map<String, Object> body = new LinkedHashMap<>();
//         body.put("model", properties.getLlm().getGpt().getModel());
//         body.put("temperature", 0.1d);
//         body.put("messages", messages(systemPrompt, userPrompt));
//         if (definitions == null || definitions.isEmpty()) {
//             body.put("response_format", Map.of("type", "json_object"));
//         } else {
//             body.put("tools", definitions.stream().map(definition -> Map.of(
//                     "type", "function",
//                     "function", Map.of(
//                             "name", definition.path("name").asString(),
//                             "description", definition.path("description").asString(),
//                             "parameters", definition.path("input_schema"))))
//                     .toList());
//             body.put("tool_choice", "required");
//         }
//         return body;
//     }

//     private List<Map<String, String>> messages(String systemPrompt, String userPrompt) {
//         if (systemPrompt == null || systemPrompt.isBlank()) {
//             return List.of(Map.of("role", "user", "content", userPrompt));
//         }
//         return List.of(
//                 Map.of("role", "system", "content", systemPrompt),
//                 Map.of("role", "user", "content", userPrompt));
//     }

//     private boolean isRetryableStatus(HttpStatusCode status) {
//         return status.value() == 408 || status.value() == 409 || status.value() == 429 || status.is5xxServerError();
//     }

//     private boolean isTimeout(Throwable throwable) {
//         for (Throwable current = throwable; current != null; current = current.getCause()) {
//             if (current instanceof SocketTimeoutException || current instanceof InterruptedIOException) return true;
//             String message = current.getMessage();
//             if (message != null && message.toLowerCase(Locale.ROOT).contains("timed out")) return true;
//         }
//         return false;
//     }

//     private String responseBody(RestClientResponseException exception) {
//         String body = exception.getResponseBodyAsString();
//         return body == null || body.isBlank() ? "<empty>" : body.substring(0, Math.min(body.length(), 1000));
//     }

//     private String rootMessage(Throwable throwable) {
//         Throwable root = throwable;
//         while (root.getCause() != null) root = root.getCause();
//         return root.getMessage() == null ? root.getClass().getSimpleName() : root.getMessage();
//     }
// }
