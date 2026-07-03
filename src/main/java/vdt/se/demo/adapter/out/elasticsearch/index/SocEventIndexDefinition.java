package vdt.se.demo.adapter.out.elasticsearch.index;

import org.springframework.stereotype.Component;
@Component
public class SocEventIndexDefinition {

    public String json() {
        return """
                {
                  "mappings": {
                    "properties": {
                %s
                    }
                  }
                }
                """.formatted(SocEventElasticsearchMapping.propertiesJson());
    }

    public String mappingJson() {
        return """
                {
                  "properties": {
                %s
                  }
                }
                """.formatted(SocEventElasticsearchMapping.propertiesJson());
    }
}
