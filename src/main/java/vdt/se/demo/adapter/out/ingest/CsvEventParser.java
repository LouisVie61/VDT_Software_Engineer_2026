package vdt.se.demo.adapter.out.ingest;

import vdt.se.demo.domain.exception.BadQueryException;
import vdt.se.demo.domain.model.SocEvent;
import vdt.se.demo.domain.service.SocEventMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CsvEventParser {

    private final SocEventMapper eventMapper;

    public CsvEventParser(SocEventMapper eventMapper) {
        this.eventMapper = eventMapper;
    }

    public List<String> parseHeaders(String line) {
        return parseValues(line).stream().map(String::trim).toList();
    }

    public SocEvent parse(List<String> headers, String line) {
        List<String> values = parseValues(line);
        if (values.size() != headers.size()) {
            throw new BadQueryException("Malformed CSV row: column count does not match header");
        }
        Map<String, String> row = new LinkedHashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            row.put(headers.get(i), values.get(i));
        }
        return eventMapper.fromCsv(row);
    }

    private List<String> parseValues(String line) {
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
}
