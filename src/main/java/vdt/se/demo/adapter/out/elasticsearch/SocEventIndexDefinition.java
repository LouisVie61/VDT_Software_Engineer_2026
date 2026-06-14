package vdt.se.demo.adapter.out.elasticsearch;

import org.springframework.stereotype.Component;

@Component
public class SocEventIndexDefinition {

    public String json() {
        return """
                {
                  "mappings": {
                    "properties": {
                      "timestamp": { "type": "date" },
                      "source": { "type": "keyword" },
                      "severity": { "type": "keyword" },
                      "event_type": { "type": "keyword" },
                      "user": { "type": "keyword" },
                      "host": { "type": "keyword" },
                      "ip": { "type": "ip" },
                      "src_ip": { "type": "ip" },
                      "dst_ip": { "type": "ip" },
                      "action": { "type": "keyword" },
                      "message": { "type": "text" },
                      "raw": { "type": "text" },
                      "metadata": { "type": "object", "enabled": true }
                    }
                  }
                }
                """;
    }
}
