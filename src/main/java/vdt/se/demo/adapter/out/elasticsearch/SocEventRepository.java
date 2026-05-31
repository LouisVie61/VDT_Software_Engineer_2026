package vdt.se.demo.adapter.out.elasticsearch;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface SocEventRepository extends ElasticsearchRepository<SocEventDocument, String> {
}
