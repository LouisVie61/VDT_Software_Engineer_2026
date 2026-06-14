package vdt.se.demo.adapter.out.ingest;

import vdt.se.demo.domain.model.SocEvent;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.function.Consumer;

public class JsonlEventFileReader implements EventFileReader {

    private final JsonlEventParser parser;

    public JsonlEventFileReader(JsonlEventParser parser) {
        this.parser = parser;
    }

    @Override
    public EventParseResult read(BufferedReader reader, Consumer<SocEvent> eventConsumer) throws IOException {
        long totalRows = 0;
        long failedRows = 0;
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.isBlank()) {
                continue;
            }
            totalRows++;
            SocEvent event;
            try {
                event = parser.parse(line);
            } catch (Exception ignored) {
                failedRows++;
                continue;
            }
            eventConsumer.accept(event);
        }
        return new EventParseResult(totalRows, failedRows);
    }
}
