package vdt.se.demo.adapter.in.rest.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import vdt.se.demo.adapter.config.AppProperties;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;

@Component
public class EventIngestRateLimitFilter extends OncePerRequestFilter {

    private static final String INGEST_PATH = "/api/events/import-file";

    private final AppProperties properties;
    private final FixedWindowRateLimiter rateLimiter;

    @Autowired
    public EventIngestRateLimitFilter(AppProperties properties) {
        this(properties, new FixedWindowRateLimiter(Clock.systemUTC()));
    }

    EventIngestRateLimitFilter(AppProperties properties, FixedWindowRateLimiter rateLimiter) {
        this.properties = properties;
        this.rateLimiter = rateLimiter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!shouldRateLimit(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        AppProperties.RateLimit limit = properties.getIngest().getRateLimit();
        if (rateLimiter.allow(clientKey(request), limit.getMaxRequests(), limit.getWindowSeconds())) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("""
                {"timestamp":"%s","status":429,"error":"Too Many Requests","message":"Event ingest rate limit exceeded"}
                """.formatted(Instant.now()));
    }

    private boolean shouldRateLimit(HttpServletRequest request) {
        return properties.getIngest().getRateLimit().isEnabled()
                && "POST".equalsIgnoreCase(request.getMethod())
                && INGEST_PATH.equals(request.getRequestURI());
    }

    private String clientKey(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
