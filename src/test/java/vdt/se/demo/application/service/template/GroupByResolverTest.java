package vdt.se.demo.application.service.template;

import org.junit.jupiter.api.Test;
import vdt.se.demo.domain.model.SocEventSchema;

import static org.assertj.core.api.Assertions.assertThat;

class GroupByResolverTest {

    private final GroupByResolver resolver = new GroupByResolver();

    @Test
    void resolvesVietnameseLocationAliasesAfterUnicodeNormalization() {
        assertThat(resolver.resolve("thống kê theo địa điểm")).contains(SocEventSchema.GEO_LOCATION);
        assertThat(resolver.resolve("top sự kiện theo vị trí")).contains(SocEventSchema.GEO_LOCATION);
    }

    @Test
    void handlesNullQuery() {
        assertThat(resolver.resolve(null)).isEmpty();
    }
}
