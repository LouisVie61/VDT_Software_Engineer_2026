package vdt.se.demo.domain.model;

import java.util.List;
import java.util.Set;

public final class SocEventSchema {
    public static final String TIMESTAMP = "timestamp";
    public static final String SOURCE = "source";
    public static final String SEVERITY = "severity";
    public static final String EVENT_TYPE = "event_type";
    public static final String ACTION = "action";
    public static final String USER = "user";
    public static final String HOST = "host";
    public static final String IP = "ip";
    public static final String GEO_LOCATION = "geo_location";
    public static final String USER_AGENT = "user_agent";
    public static final String MESSAGE = "message";
    public static final String RAW = "raw";
    public static final String METADATA = "metadata";
    public static final String ADVANCED_METADATA = "advanced_metadata";

    public static final List<String> MINIMUM_EVENT_FIELDS = List.of(
            TIMESTAMP, SOURCE, SEVERITY, EVENT_TYPE, USER, HOST, IP, MESSAGE, RAW
    );

    public static final List<String> ENRICHED_EVENT_FIELDS = List.of(
            GEO_LOCATION, USER_AGENT
    );

    public static final List<String> INDEX_FIELDS = List.of(
            TIMESTAMP, SOURCE, SEVERITY, EVENT_TYPE, ACTION, USER, HOST, IP,
            GEO_LOCATION, USER_AGENT, MESSAGE, RAW, METADATA
    );

    public static final List<String> CSV_HEADER_FIELDS = List.of(
            TIMESTAMP, SOURCE, SEVERITY, EVENT_TYPE, ACTION, USER, HOST, IP, MESSAGE, RAW, ADVANCED_METADATA
    );

    public static final Set<String> FIELD_WHITELIST = Set.of(
            TIMESTAMP, SOURCE, SEVERITY, EVENT_TYPE, ACTION, USER, HOST, IP,
            GEO_LOCATION, USER_AGENT, MESSAGE, RAW
    );

    public static final List<String> FILTERABLE_FIELDS = List.of(
            SEVERITY, EVENT_TYPE, ACTION, USER, HOST, IP, SOURCE, GEO_LOCATION, USER_AGENT
    );

    public static final List<String> GROUPABLE_FIELDS = List.of(
            SOURCE, SEVERITY, EVENT_TYPE, ACTION, USER, HOST, IP, GEO_LOCATION, USER_AGENT
    );

    public static final List<String> FULL_TEXT_FIELDS = List.of(MESSAGE, RAW, USER_AGENT);

    public static final List<String> RESPONSE_FIELDS = List.of(
            TIMESTAMP, SOURCE, SEVERITY, EVENT_TYPE, ACTION, USER, HOST, IP,
            GEO_LOCATION, USER_AGENT, MESSAGE, RAW
    );

    private SocEventSchema() {
    }

    public static String canonicalCsvHeader() {
        return String.join(",", CSV_HEADER_FIELDS);
    }
}
