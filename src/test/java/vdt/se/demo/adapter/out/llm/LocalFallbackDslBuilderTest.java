package vdt.se.demo.adapter.out.llm;

import org.junit.jupiter.api.Test;
import vdt.se.demo.application.dto.SearchRequest;

import static org.assertj.core.api.Assertions.assertThat;

class LocalFallbackDslBuilderTest {

    @Test
    void buildsKeywordDslWithExplicitFiltersAndPagination() {
        SearchRequest request = new SearchRequest();
        request.setQuestion("show failed login");
        request.setSeverity("high");
        request.setIp("10.0.0.1");
        request.setPage(2);
        request.setPageSize(25);

        String dsl = new LocalFallbackDslBuilder().build(request);

        assertThat(dsl).contains("\"query\": \"failed login auth\"");
        assertThat(dsl).contains("\"term\":{\"severity\":\"high\"}");
        assertThat(dsl).contains("\"src_ip\":\"10.0.0.1\"");
        assertThat(dsl).contains("\"from\": 50");
        assertThat(dsl).contains("\"size\": 25");
    }
}
