package vdt.se.demo.adapter.out.ingest;

import vdt.se.demo.domain.model.SocEvent;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

public class CsvEventFileReader implements EventFileReader {

    private final CsvEventParser parser;

    public CsvEventFileReader(CsvEventParser parser) {
        this.parser = parser;
    }

    @Override
    public EventParseResult read(BufferedReader reader, Consumer<SocEvent> eventConsumer) throws IOException {
        String headerLine = reader.readLine();
        if (headerLine == null || headerLine.isBlank()) {
            return new EventParseResult(0, 0);
        }

        List<String> headers = parser.parseHeaders(headerLine);
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
                event = parser.parse(headers, line);
            } catch (Exception ignored) {
                failedRows++;
                continue;
            }
            eventConsumer.accept(event);
        }
        return new EventParseResult(totalRows, failedRows);
    }
}
