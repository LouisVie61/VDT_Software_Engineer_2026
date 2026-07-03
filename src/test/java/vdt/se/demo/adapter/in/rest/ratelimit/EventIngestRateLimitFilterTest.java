package vdt.se.demo.adapter.in.rest.ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import vdt.se.demo.adapter.config.AppProperties;

import static org.assertj.core.api.Assertions.assertThat;

class EventIngestRateLimitFilterTest {

    @Test
    void rejectsImportRequestsAfterConfiguredWindowLimit() throws Exception {
        AppProperties properties = new AppProperties();
        properties.getIngest().getRateLimit().setMaxRequests(1);
        properties.getIngest().getRateLimit().setWindowSeconds(60);
        EventIngestRateLimitFilter filter = new EventIngestRateLimitFilter(
                properties, new FixedWindowRateLimiter(java.time.Clock.systemUTC()));

        MockHttpServletRequest first = request();
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        filter.doFilter(first, firstResponse, new MockFilterChain());

        MockHttpServletRequest second = request();
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        filter.doFilter(second, secondResponse, new MockFilterChain());

        assertThat(firstResponse.getStatus()).isEqualTo(200);
        assertThat(secondResponse.getStatus()).isEqualTo(429);
        assertThat(secondResponse.getContentAsString()).contains("rate limit exceeded");
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/events/import-file");
        request.setRemoteAddr("10.0.0.10");
        return request;
    }
}
