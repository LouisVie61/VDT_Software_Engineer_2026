package vdt.se.demo.application.service.intent;

import org.junit.jupiter.api.Test;
import vdt.se.demo.domain.exception.BadQueryException;
import vdt.se.demo.domain.model.SearchIntent;
import vdt.se.demo.domain.valueObjects.TemplateType;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfirmationIntentValidatorTest {
    private final ConfirmationIntentValidator validator =
            new ConfirmationIntentValidator(new SearchSchemaRegistry());

    @Test
    void rejectsNonGroupableFieldBeforePlanBuild() {
        SearchIntent intent = SearchIntent.builder()
                .intent(TemplateType.TERMS_AGGREGATION)
                .groupBy("message")
                .topN(10)
                .build();

        assertThatThrownBy(() -> validator.validate(intent))
                .isInstanceOf(BadQueryException.class)
                .hasMessageContaining("not groupable");
    }

    @Test
    void rejectsTopNOutsideSupportedBounds() {
        SearchIntent intent = SearchIntent.builder()
                .intent(TemplateType.TERMS_AGGREGATION)
                .groupBy("severity")
                .topN(0)
                .build();

        assertThatThrownBy(() -> validator.validate(intent))
                .isInstanceOf(BadQueryException.class)
                .hasMessageContaining("between 1 and 100");
    }

    @Test
    void acceptsValidTermsAggregation() {
        SearchIntent intent = SearchIntent.builder()
                .intent(TemplateType.TERMS_AGGREGATION)
                .groupBy("severity")
                .topN(10)
                .build();

        assertThatCode(() -> validator.validate(intent)).doesNotThrowAnyException();
    }
}
