package vdt.se.demo.application.dto;

import java.io.IOException;
import java.io.InputStream;

public record IngestFileCommand(
        String filename,
        String contentType,
        long size,
        InputStreamSource content
) {
    public InputStream openStream() throws IOException {
        return content.openStream();
    }

    @FunctionalInterface
    public interface InputStreamSource {
        InputStream openStream() throws IOException;
    }
}
