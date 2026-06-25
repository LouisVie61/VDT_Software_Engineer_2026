package vdt.se.demo.adapter.out.elasticsearch.query;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import vdt.se.demo.adapter.out.elasticsearch.client.ElasticsearchHttpClient;
import vdt.se.demo.application.port.outboundPort.execution.DiagnosticProbePort;

@Component
public class ElasticsearchDiagnosticProbeAdapter implements DiagnosticProbePort {
    private final ElasticsearchHttpClient httpClient;

    public ElasticsearchDiagnosticProbeAdapter(ElasticsearchHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public int count(JsonNode countDsl) {
        try {
            return httpClient.count(countDsl).path("count").asInt(0);
        } catch (Exception e) {
            return 0;
        }
    }
}
