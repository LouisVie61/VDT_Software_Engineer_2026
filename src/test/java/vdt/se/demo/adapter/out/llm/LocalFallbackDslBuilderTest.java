package vdt.se.demo.adapter.out.llm;

import org.junit.jupiter.api.Test;
import vdt.se.demo.application.dto.SearchRequest;

import static org.assertj.core.api.Assertions.assertThat;

class LocalFallbackDslBuilderTest {

    @Test
    void buildsKeywordDslWithExplicitFiltersAndPagination() {
        SearchRequest request = new SearchRequest();
        request.setQuestion("show failed login");
        request.setFrom("2026-06-01T00:00:00Z");
        request.setTo("2026-06-15T00:00:00Z");
        request.setSeverity("high");
        request.setEventType("auth");
        request.setUser("alice");
        request.setHost("host-1");
        request.setIp("10.0.0.1");
        request.setPage(2);
        request.setPageSize(25);

        String dsl = new LocalFallbackDslBuilder().build(request);

        assertThat(dsl).contains("\"query\": \"failed login auth\"");
        assertThat(dsl).contains("\"gte\":\"2026-06-01T00:00:00Z\"");
        assertThat(dsl).contains("\"lte\":\"2026-06-15T00:00:00Z\"");
        assertThat(dsl).contains("\"term\":{\"severity\":\"high\"}");
        assertThat(dsl).contains("\"term\":{\"event_type\":\"auth\"}");
        assertThat(dsl).contains("\"term\":{\"user\":\"alice\"}");
        assertThat(dsl).contains("\"term\":{\"host\":\"host-1\"}");
        assertThat(dsl).contains("\"term\":{\"ip\":\"10.0.0.1\"}");
        assertThat(dsl).contains("\"from\": 50");
        assertThat(dsl).contains("\"size\": 25");
    }
}
