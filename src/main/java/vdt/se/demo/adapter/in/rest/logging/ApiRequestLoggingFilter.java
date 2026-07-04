package vdt.se.demo.adapter.in.rest.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
public class ApiRequestLoggingFilter extends OncePerRequestFilter {
    public static final String ERROR_LOGGED_ATTRIBUTE = "vdt.se.demo.apiErrorLogged";
    public static final String ERROR_MESSAGE_ATTRIBUTE = "vdt.se.demo.apiErrorMessage";

    private static final Logger log = LoggerFactory.getLogger(ApiRequestLoggingFilter.class);

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long startedAt = System.nanoTime();
        String requestId = request.getHeader("X-Request-Id");
        if (requestId == null || requestId.isBlank()) {
            requestId = java.util.UUID.randomUUID().toString();
        }
        response.setHeader("X-Request-Id", requestId);
        String previousRequestId = MDC.get("requestId");
        MDC.put("requestId", requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            if (response.getStatus() >= 400 && !Boolean.TRUE.equals(request.getAttribute(ERROR_LOGGED_ATTRIBUTE))) {
                log.warn("HTTP error: status={}, api={} {}, message={}, elapsedMs={}, requestId={}",
                        response.getStatus(), request.getMethod(), request.getRequestURI(),
                        errorMessage(request), TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt), requestId);
            }
            if (previousRequestId == null) MDC.remove("requestId");
            else MDC.put("requestId", previousRequestId);
        }
    }

    private String errorMessage(HttpServletRequest request) {
        Object message = request.getAttribute(ERROR_MESSAGE_ATTRIBUTE);
        return message == null ? "HTTP request failed" : message.toString();
    }
}
