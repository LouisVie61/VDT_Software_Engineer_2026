package vdt.se.demo.application.port.outboundPort;

import vdt.se.demo.domain.model.SocEvent;

import java.util.List;

public interface EventIndexPort {
    void ensureIndex();

    void indexBatch(List<SocEvent> events);

    String indexName();
}
