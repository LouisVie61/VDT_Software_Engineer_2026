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

        for (String key : List.of("datasource", "elasticsearch", "provider-order", "batch-size", "multipart")) {
            assertThat(dev).contains(key);
            assertThat(example).contains(key);
        }
        assertThat(properties).contains("spring.application.name=ai-soc-search-demo");
        assertThat(properties).doesNotContain("replace-me-secret");
    }
}
