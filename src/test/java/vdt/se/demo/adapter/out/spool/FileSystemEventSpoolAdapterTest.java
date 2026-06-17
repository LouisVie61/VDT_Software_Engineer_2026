package vdt.se.demo.adapter.out.spool;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import vdt.se.demo.adapter.config.AppProperties;
import vdt.se.demo.application.dto.IngestFileCommand;
import vdt.se.demo.domain.valueObjects.EventFileFormat;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FileSystemEventSpoolAdapterTest {

    @TempDir
    private Path tempDir;

    @Test
    void writesPartialThenFinalFileInFormatFolder() throws Exception {
        AppProperties properties = new AppProperties();
        properties.getIngest().setSpoolRoot(tempDir);
        FileSystemEventSpoolAdapter adapter = new FileSystemEventSpoolAdapter(properties);
        UUID requestId = UUID.fromString("00000000-0000-0000-0000-000000000123");

        adapter.spool(command("Events Upload.JSONL", "{\"message\":\"ok\"}"), EventFileFormat.JSONL, requestId);

        Path targetDirectory = tempDir.resolve("incoming").resolve("jsonl");
        List<Path> files = Files.list(targetDirectory).toList();
        assertThat(files).hasSize(1);
        assertThat(files.getFirst().getFileName().toString())
                .isEqualTo("00000000-0000-0000-0000-000000000123-events-upload.jsonl");
        assertThat(Files.readString(files.getFirst())).isEqualTo("{\"message\":\"ok\"}");
        assertThat(Files.list(targetDirectory).noneMatch(path -> path.toString().endsWith(".part"))).isTrue();
    }

    private IngestFileCommand command(String filename, String content) {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        return new IngestFileCommand(filename, "application/x-ndjson", bytes.length,
                () -> new ByteArrayInputStream(bytes));
    }
}
