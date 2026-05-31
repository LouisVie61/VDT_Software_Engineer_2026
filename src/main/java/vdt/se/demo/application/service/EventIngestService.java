package vdt.se.demo.application.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.adapter.config.AppProperties;
import vdt.se.demo.adapter.out.elasticsearch.SocEventDocument;
import vdt.se.demo.adapter.out.elasticsearch.SocEventRepository;
import vdt.se.demo.domain.exception.BadQueryException;
import vdt.se.demo.domain.model.IngestResult;
import vdt.se.demo.domain.service.EventDocumentMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class EventIngestService {

    private final ObjectMapper objectMapper;
    private final EventDocumentMapper mapper;
    private final SocEventRepository repository;
    private final AppProperties properties;

    public EventIngestService(ObjectMapper objectMapper, EventDocumentMapper mapper, SocEventRepository repository,
                              AppProperties properties) {
        this.objectMapper = objectMapper;
        this.mapper = mapper;
        this.repository = repository;
        this.properties = properties;
    }

    public IngestResult ingestFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadQueryException("Uploaded event file is empty");
        }

        EventFileFormat format = detectFormat(file);
        Instant started = Instant.now();
        Counters counters = switch (format) {
            case JSONL -> ingestJsonl(file);
            case CSV -> ingestCsv(file);
        };

        if (counters.totalRows == 0) {
            throw new BadQueryException("Uploaded event file has no event rows");
        }

        return new IngestResult(
                counters.totalRows,
                counters.indexedRows,
                counters.failedRows,
                properties.getElasticsearch().getEventsIndex(),
                Duration.between(started, Instant.now()).toMillis()
        );
    }

    private Counters ingestJsonl(MultipartFile file) {
        Counters counters = new Counters();
        List<SocEventDocument> batch = new ArrayList<>();

        try (BufferedReader reader = reader(file)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                counters.totalRows++;
                try {
                    JsonNode node = objectMapper.readTree(line);
                    batch.add(mapper.fromJson(node));
                    if (batch.size() >= properties.getIngest().getBatchSize()) {
                        counters.indexedRows += flush(batch);
                    }
                } catch (Exception ignored) {
                    counters.failedRows++;
                }
            }
            counters.indexedRows += flush(batch);
        } catch (Exception e) {
            throw new BadQueryException("Cannot read uploaded event file", e);
        }

        return counters;
    }

    private Counters ingestCsv(MultipartFile file) {
        Counters counters = new Counters();
        List<SocEventDocument> batch = new ArrayList<>();

        try (BufferedReader reader = reader(file)) {
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                return counters;
            }

            List<String> headers = parseCsvLine(headerLine);
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                counters.totalRows++;
                try {
                    List<String> values = parseCsvLine(line);
                    if (values.size() != headers.size()) {
                        counters.failedRows++;
                        continue;
                    }
                    batch.add(mapper.fromMap(toRow(headers, values)));
                    if (batch.size() >= properties.getIngest().getBatchSize()) {
                        counters.indexedRows += flush(batch);
                    }
                } catch (Exception ignored) {
                    counters.failedRows++;
                }
            }
            counters.indexedRows += flush(batch);
        } catch (Exception e) {
            throw new BadQueryException("Cannot read uploaded event file", e);
        }

        return counters;
    }

    private BufferedReader reader(MultipartFile file) throws Exception {
        return new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
    }

    private EventFileFormat detectFormat(MultipartFile file) {
        String filename = file.getOriginalFilename();
        String normalizedName = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        if (normalizedName.endsWith(".jsonl")) {
            return EventFileFormat.JSONL;
        }
        if (normalizedName.endsWith(".csv")) {
            return EventFileFormat.CSV;
        }

        String contentType = file.getContentType();
        String normalizedContentType = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        if (normalizedContentType.contains("csv")) {
            return EventFileFormat.CSV;
        }
        if (normalizedContentType.contains("json")) {
            return EventFileFormat.JSONL;
        }

        throw new BadQueryException("Unsupported event file format. Supported formats: .jsonl, .csv");
    }

    private Map<String, String> toRow(List<String> headers, List<String> values) {
        Map<String, String> row = new LinkedHashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            row.put(headers.get(i).trim(), values.get(i));
        }
        return row;
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (ch == ',' && !quoted) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }

        if (quoted) {
            throw new BadQueryException("Malformed CSV row: unclosed quote");
        }

        values.add(current.toString());
        return values;
    }

    private int flush(List<SocEventDocument> batch) {
        if (batch.isEmpty()) {
            return 0;
        }
        int size = batch.size();
        repository.saveAll(batch);
        batch.clear();
        return size;
    }

    private enum EventFileFormat {
        JSONL,
        CSV
    }

    private static class Counters {
        long totalRows = 0;
        long indexedRows = 0;
        long failedRows = 0;
    }
}
