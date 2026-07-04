package vdt.se.demo.adapter.in.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import vdt.se.demo.adapter.in.rest.logging.ApiRequestLoggingFilter;
import vdt.se.demo.domain.exception.BadQueryException;
import vdt.se.demo.domain.exception.LlmException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class RestExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

    @ExceptionHandler(BadQueryException.class)
    ResponseEntity<Map<String, Object>> badQuery(BadQueryException exception, HttpServletRequest request) {
        if ("CLARIFICATION_REQUIRED".equals(exception.getReasonCode())) {
            return clarification(exception, request);
        }
        ResponseEntity<Map<String, Object>> response = error(HttpStatus.BAD_REQUEST, exception.getReasonCode(), exception.getMessage(), request, exception);
        if (exception.getGeneratedDsl() == null) return response;
        Map<String, Object> body = new LinkedHashMap<>(response.getBody());
        body.put("generatedDsl", exception.getGeneratedDsl());
        return ResponseEntity.status(response.getStatusCode()).body(body);
    }

    private ResponseEntity<Map<String, Object>> clarification(BadQueryException exception, HttpServletRequest request) {
        String question = exception.getMessage() == null || exception.getMessage().isBlank()
                ? "Please clarify your request."
                : exception.getMessage();
        log.info("Clarification required: api={} {}, question={}",
                request.getMethod(), request.getRequestURI(), question);
        return ResponseEntity.ok(Map.of(
                "timestamp", Instant.now().toString(),
                "status", HttpStatus.OK.value(),
                "reasonCode", "CLARIFICATION_REQUIRED",
                "needsClarification", true,
                "question", question,
                "message", question
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        List<Map<String, Object>> violations = exception.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> {
                    Map<String, Object> violation = new LinkedHashMap<>();
                    violation.put("field", fieldError.getField());
                    violation.put("message", fieldError.getDefaultMessage());
                    violation.put("rejectedValue", safeRejectedValue(fieldError.getRejectedValue()));
                    return violation;
                }).toList();
        String detail = violations.isEmpty()
                ? "Request validation failed"
                : violations.stream()
                        .map(item -> item.get("field") + " " + item.get("message"))
                        .collect(java.util.stream.Collectors.joining("; "));

        request.setAttribute(ApiRequestLoggingFilter.ERROR_LOGGED_ATTRIBUTE, true);
        request.setAttribute(ApiRequestLoggingFilter.ERROR_MESSAGE_ATTRIBUTE, detail);
        log.warn("REQUEST_VALIDATION_FAILED api={} {}, violations={}",
                request.getMethod(), request.getRequestURI(), violations);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", HttpStatus.BAD_REQUEST.getReasonPhrase());
        body.put("reasonCode", "REQUEST_VALIDATION_FAILED");
        body.put("message", detail);
        body.put("violations", violations);
        return ResponseEntity.badRequest().body(body);
    }

    private Object safeRejectedValue(Object value) {
        if (value == null) return null;
        String text = value.toString();
        return text.length() <= 200 ? text : text.substring(0, 200) + "...";
    }

    @ExceptionHandler(LlmException.class)
    ResponseEntity<Map<String, Object>> llm(LlmException exception, HttpServletRequest request) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, "LLM_PROVIDER_UNAVAILABLE", exception.getMessage(), request, exception);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> generic(Exception exception, HttpServletRequest request) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", exception.getMessage(), request, exception);
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String reasonCode, String message,
                                                      HttpServletRequest request, Exception exception) {
        String safeMessage = message == null || message.isBlank() ? status.getReasonPhrase() : message;
        request.setAttribute(ApiRequestLoggingFilter.ERROR_LOGGED_ATTRIBUTE, true);
        request.setAttribute(ApiRequestLoggingFilter.ERROR_MESSAGE_ATTRIBUTE, safeMessage);
        if (status.is5xxServerError()) {
            log.error("HTTP error: status={}, api={} {}, message={}",
                    status.value(), request.getMethod(), request.getRequestURI(), safeMessage);
            log.debug("HTTP error stacktrace", exception);
        } else {
            log.warn("HTTP error: status={}, api={} {}, message={}",
                    status.value(), request.getMethod(), request.getRequestURI(), safeMessage);
        }
        return ResponseEntity.status(status).body(Map.of(
                "timestamp", Instant.now().toString(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "reasonCode", reasonCode,
                "message", safeMessage
        ));
    }
}
