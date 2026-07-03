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
import java.util.Map;

@RestControllerAdvice
public class RestExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

    @ExceptionHandler(BadQueryException.class)
    ResponseEntity<Map<String, Object>> badQuery(BadQueryException exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage(), request, exception);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "Request validation failed", request, exception);
    }

    @ExceptionHandler(LlmException.class)
    ResponseEntity<Map<String, Object>> llm(LlmException exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_GATEWAY, exception.getMessage(), request, exception);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> generic(Exception exception, HttpServletRequest request) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage(), request, exception);
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message,
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
                "message", safeMessage
        ));
    }
}
