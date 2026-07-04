package vdt.se.demo.adapter.in.rest;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.application.dto.SearchRequest;
import vdt.se.demo.domain.exception.BadQueryException;

import static org.assertj.core.api.Assertions.assertThat;

class RestExceptionHandlerTest {
    @Test
    void exposesFieldLevelRequestValidationErrors() throws Exception {
        SearchRequest target = new SearchRequest();
        var binding = new BeanPropertyBindingResult(target, "searchRequest");
        binding.addError(new FieldError("searchRequest", "question", "", false,
                null, null, "must not be blank"));
        var parameter = new MethodParameter(
                QueryController.class.getMethod("search", SearchRequest.class), 0);
        var exception = new MethodArgumentNotValidException(parameter, binding);

        var response = new RestExceptionHandler().validation(exception,
                new MockHttpServletRequest("POST", "/api/search"));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody())
                .containsEntry("reasonCode", "REQUEST_VALIDATION_FAILED")
                .containsEntry("message", "question must not be blank");
        assertThat((java.util.List<?>) response.getBody().get("violations")).hasSize(1);
    }

    @Test
    void returnsOkWhenLlmNeedsTheUserToClarify() {
        var request = new MockHttpServletRequest("POST", "/api/search");

        var response = new RestExceptionHandler().badQuery(
                new BadQueryException("CLARIFICATION_REQUIRED", "Which specific year in November are you referring to?"),
                request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody())
                .containsEntry("reasonCode", "CLARIFICATION_REQUIRED")
                .containsEntry("needsClarification", true)
                .containsEntry("question", "Which specific year in November are you referring to?");
    }

    @Test
    void includesGeneratedDslForRejectedLlmQuery() {
        ObjectMapper mapper = new ObjectMapper();
        var dsl = mapper.readTree("{\"size\":0,\"aggs\":{\"events\":{\"terms\":{\"field\":\"timestamp\"}}}}");
        var request = new MockHttpServletRequest("POST", "/api/search");

        var response = new RestExceptionHandler().badQuery(
                new BadQueryException("BAD_QUERY", "Field is not groupable: timestamp", dsl), request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).containsEntry("generatedDsl", dsl);
    }
}
