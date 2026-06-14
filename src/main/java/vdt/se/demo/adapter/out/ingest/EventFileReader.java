package vdt.se.demo.adapter.out.ingest;

import vdt.se.demo.domain.model.SocEvent;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.function.Consumer;

public interface EventFileReader {
    EventParseResult read(BufferedReader reader, Consumer<SocEvent> eventConsumer) throws IOException;
}
