package vdt.se.demo;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigFilesTest {

    @Test
    void schemaContainsMvpTablesAndFakeUser() throws Exception {
        String schema = Files.readString(Path.of("src/main/resources/schema.sql"));

        assertThat(schema).contains("app_users");
        assertThat(schema).contains("query_history");
        assertThat(schema).contains("audit_logs");
        assertThat(schema).contains("soc-analyst-demo");
    }

    @Test
    void devAndExampleConfigsExposeSameMvpKeys() throws Exception {
        String dev = Files.readString(Path.of("src/main/resources/application-dev.yml"));
        String example = Files.readString(Path.of("src/main/resources/application-example.yml"));
        String properties = Files.readString(Path.of("src/main/resources/application.properties"));

        for (String key : List.of("datasource", "elasticsearch", "provider-order", "spool-root", "rate-limit", "multipart")) {
            assertThat(dev).contains(key);
            assertThat(example).contains(key);
        }
        assertThat(properties).contains("spring.application.name=ai-soc-search-demo");
        assertThat(properties).doesNotContain("replace-me-secret");
    }

    @Test
    void fluentdPromotesActionAndNormalizesDatasetTextFields() throws Exception {
        String fluentd = Files.readString(Path.of("fluentd/fluent.conf"));

        assertThat(fluentd).contains("keys event_id,timestamp,source,severity,event_type,action,user,host,ip,message,raw,advanced_metadata");
        assertThat(fluentd).contains("event_id ${record['event_id']}");
        assertThat(fluentd).contains("action ${record['action']}");
        assertThat(fluentd).contains("timestamp_year ${m=record['timestamp']");
        assertThat(fluentd).contains("timestamp_second ${m=record['timestamp']");
        assertThat(fluentd).contains("record['raw_log']");
        assertThat(fluentd).contains("record['description']");
    }
}
