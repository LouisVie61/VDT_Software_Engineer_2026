package vdt.se.demo.adapter.out.spool;

import org.springframework.stereotype.Component;
import vdt.se.demo.adapter.config.AppProperties;
import vdt.se.demo.application.dto.IngestFileCommand;
import vdt.se.demo.application.port.outboundPort.EventSpoolPort;
import vdt.se.demo.domain.exception.BadQueryException;
import vdt.se.demo.domain.valueObjects.EventFileFormat;

import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

@Component
public class FileSystemEventSpoolAdapter implements EventSpoolPort {

    private final AppProperties properties;

    public FileSystemEventSpoolAdapter(AppProperties properties) {
        this.properties = properties;
    }

    @Override
    public void spool(IngestFileCommand command, EventFileFormat format, UUID requestId) {
        Path targetDirectory = targetDirectory(format);
        Path partialFile = targetDirectory.resolve(fileName(command, format, requestId) + ".part");
        Path finalFile = targetDirectory.resolve(fileName(command, format, requestId));
        try {
            Files.createDirectories(targetDirectory);
            Files.copy(command.openStream(), partialFile, StandardCopyOption.REPLACE_EXISTING);
            moveCompleteFile(partialFile, finalFile);
        } catch (Exception e) {
            deleteQuietly(partialFile);
            throw new BadQueryException("Cannot spool uploaded event file for Fluentd", e);
        }
    }

    private Path targetDirectory(EventFileFormat format) {
        return properties.getIngest().getSpoolRoot()
                .resolve("incoming")
                .resolve(format.directoryName());
    }

    private String fileName(IngestFileCommand command, EventFileFormat format, UUID requestId) {
        return requestId + "-" + sanitize(command.filename()) + format.extension();
    }

    private String sanitize(String filename) {
        String name = filename == null || filename.isBlank() ? "events" : Path.of(filename).getFileName().toString();
        int extensionIndex = name.lastIndexOf('.');
        if (extensionIndex > 0) {
            name = name.substring(0, extensionIndex);
        }
        String sanitized = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]+", "-");
        return sanitized.isBlank() ? "events" : sanitized;
    }

    private void moveCompleteFile(Path partialFile, Path finalFile) throws Exception {
        try {
            Files.move(partialFile, finalFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(partialFile, finalFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (Exception ignored) {
        }
    }
}
