package vdt.se.demo.application.service.cache;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.domain.iql.IqlQuery;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class IqlCacheKeyServiceTest {
    @Test
    void excludesPaginationCursorFromCacheKey() {
        ObjectMapper mapper=new ObjectMapper();
        IqlCacheKeyService service=new IqlCacheKeyService(mapper, "mapping-v1");
        IqlQuery first=new IqlQuery(List.of(),List.of(),null,null,List.of(new IqlQuery.GroupBy("source",10)),
                List.of(),null,List.of(),50,null);
        IqlQuery next=new IqlQuery(first.select(),first.filters(),null,null,first.groupBy(),first.metrics(),null,
                first.sort(),first.size(),Map.of("source",mapper.valueToTree("firewall")));
        assertThat(service.key(first)).isEqualTo(service.key(next));
    }

    @Test
    void mappingVersionInvalidatesCachedDsl() {
        ObjectMapper mapper = new ObjectMapper();
        IqlQuery query = new IqlQuery(List.of(), List.of(), null, null, List.of(), List.of(), null, List.of(), 50, null);
        assertThat(new IqlCacheKeyService(mapper, "mapping-v1").key(query))
                .isNotEqualTo(new IqlCacheKeyService(mapper, "mapping-v2").key(query));
    }

}
