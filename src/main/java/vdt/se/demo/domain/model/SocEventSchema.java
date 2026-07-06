package vdt.se.demo.domain.model;

import java.util.List;
import java.util.Set;

public final class SocEventSchema {
    public static final String EVENT_ID = "event_id";
    public static final String TIMESTAMP = "timestamp";
    public static final String TIMESTAMP_YEAR = "timestamp_year";
    public static final String TIMESTAMP_MONTH = "timestamp_month";
    public static final String TIMESTAMP_DAY = "timestamp_day";
    public static final String TIMESTAMP_HOUR = "timestamp_hour";
    public static final String TIMESTAMP_MINUTE = "timestamp_minute";
    public static final String TIMESTAMP_SECOND = "timestamp_second";
    public static final String TIMESTAMP_QUARTER = "timestamp_quarter";
    public static final String TIMESTAMP_DATE = "timestamp_date";
    public static final String TIMESTAMP_DAY_OF_WEEK = "timestamp_day_of_week";
    public static final String TIMESTAMP_IS_WEEKEND = "timestamp_is_weekend";
    public static final String SOURCE = "source";
    public static final String SOURCE_PRODUCT = "source_product";
    public static final String SOURCE_VERSION = "source_version";
    public static final String SEVERITY = "severity";
    public static final String SEVERITY_RANK = "severity_rank";
    public static final String EVENT_TYPE = "event_type";
    public static final String ACTION = "action";
    public static final String USER = "user";
    public static final String HOST = "host";
    public static final String IP = "ip";
    public static final String GEO_LOCATION = "geo_location";
    public static final String USER_AGENT = "user_agent";
    public static final String USER_AGENT_FAMILY = "user_agent_family";
    public static final String USER_AGENT_OS = "user_agent_os";
    public static final String SRC_IP = "src_ip";
    public static final String DST_IP = "dst_ip";
    public static final String SRC_IP_PREFIX24 = "src_ip_prefix24";
    public static final String DST_IP_PREFIX24 = "dst_ip_prefix24";
    public static final String NETWORK_PAIR = "network_pair";
    public static final String ALERT_TYPE = "alert_type";
    public static final String SIGNATURE_ID = "signature_id";
    public static final String CATEGORY = "category";
    public static final String DEVICE_TYPE = "device_type";
    public static final String DEVICE_ID = "device_id";
    public static final String FIRMWARE_VERSION = "firmware_version";
    public static final String OBJECT = "object";
    public static final String PROCESS_ID = "process_id";
    public static final String PARENT_PROCESS = "parent_process";
    public static final String ADDITIONAL_INFO = "additional_info";
    public static final String DESCRIPTION = "description";
    public static final String RAW_LOG = "raw_log";
    public static final String DEVICE_HASH = "device_hash";
    public static final String SESSION_ID = "session_id";
    public static final String RISK_SCORE = "risk_score";
    public static final String RISK_LEVEL = "risk_level";
    public static final String CONFIDENCE = "confidence";
    public static final String CONFIDENCE_LEVEL = "confidence_level";
    public static final String BASELINE_DEVIATION = "baseline_deviation";
    public static final String ENTROPY = "entropy";
    public static final String FREQUENCY_ANOMALY = "frequency_anomaly";
    public static final String SEQUENCE_ANOMALY = "sequence_anomaly";
    public static final String HAS_BEHAVIORAL_ANOMALY = "has_behavioral_anomaly";
    public static final String EVENT_TYPE_ACTION = "event_type_action";
    public static final String SRC_PORT = "src_port";
    public static final String DST_PORT = "dst_port";
    public static final String PROTOCOL = "protocol";
    public static final String BYTES = "bytes";
    public static final String DURATION = "duration";
    public static final String CLOUD_SERVICE = "cloud_service";
    public static final String RESOURCE_ID = "resource_id";
    public static final String METHOD = "method";
    public static final String MODEL_ID = "model_id";
    public static final String INPUT_HASH = "input_hash";
    public static final String OUTPUT_HASH = "output_hash";
    public static final String MAC_ADDRESS = "mac_address";
    public static final String MESSAGE = "message";
    public static final String RAW = "raw";
    public static final String METADATA = "metadata";
    public static final String ADVANCED_METADATA = "advanced_metadata";

    public static final List<String> INDEX_FIELDS = List.of(
            EVENT_ID, TIMESTAMP, TIMESTAMP_YEAR, TIMESTAMP_MONTH, TIMESTAMP_DAY, TIMESTAMP_QUARTER,
            TIMESTAMP_HOUR, TIMESTAMP_MINUTE, TIMESTAMP_SECOND, TIMESTAMP_DATE, TIMESTAMP_DAY_OF_WEEK,
            TIMESTAMP_IS_WEEKEND, SOURCE, SOURCE_PRODUCT, SOURCE_VERSION, SEVERITY, SEVERITY_RANK,
            EVENT_TYPE, ACTION, EVENT_TYPE_ACTION, USER, HOST, IP, GEO_LOCATION, USER_AGENT,
            USER_AGENT_FAMILY, USER_AGENT_OS, SRC_IP, DST_IP, SRC_IP_PREFIX24, DST_IP_PREFIX24, NETWORK_PAIR,
            ALERT_TYPE, SIGNATURE_ID, CATEGORY, DEVICE_TYPE, DEVICE_ID, FIRMWARE_VERSION, OBJECT, PROCESS_ID,
            PARENT_PROCESS, ADDITIONAL_INFO, DESCRIPTION, RAW_LOG, DEVICE_HASH, SESSION_ID, RISK_SCORE, RISK_LEVEL,
            CONFIDENCE, CONFIDENCE_LEVEL, BASELINE_DEVIATION, ENTROPY, FREQUENCY_ANOMALY, SEQUENCE_ANOMALY,
            HAS_BEHAVIORAL_ANOMALY, SRC_PORT, DST_PORT, PROTOCOL, BYTES, DURATION, CLOUD_SERVICE, RESOURCE_ID,
            METHOD, MODEL_ID, INPUT_HASH, OUTPUT_HASH, MAC_ADDRESS, MESSAGE, RAW, METADATA, ADVANCED_METADATA
    );

    public static final List<String> CSV_HEADER_FIELDS = List.of(
            EVENT_ID, TIMESTAMP, SOURCE, SEVERITY, EVENT_TYPE, ACTION, USER, HOST, IP, MESSAGE, RAW, ADVANCED_METADATA
    );

    public static final Set<String> FIELD_WHITELIST = Set.of(
            EVENT_ID, TIMESTAMP, TIMESTAMP_YEAR, TIMESTAMP_MONTH, TIMESTAMP_DAY, TIMESTAMP_QUARTER,
            TIMESTAMP_HOUR, TIMESTAMP_MINUTE, TIMESTAMP_SECOND, TIMESTAMP_DATE, TIMESTAMP_DAY_OF_WEEK,
            TIMESTAMP_IS_WEEKEND, SOURCE, SOURCE_PRODUCT, SOURCE_VERSION, SEVERITY, SEVERITY_RANK,
            EVENT_TYPE, ACTION, EVENT_TYPE_ACTION, USER, HOST, IP, GEO_LOCATION, USER_AGENT, USER_AGENT_FAMILY,
            USER_AGENT_OS, SRC_IP, DST_IP, SRC_IP_PREFIX24, DST_IP_PREFIX24, NETWORK_PAIR, ALERT_TYPE, SIGNATURE_ID,
            CATEGORY, DEVICE_TYPE, DEVICE_ID, FIRMWARE_VERSION, OBJECT, PROCESS_ID, PARENT_PROCESS, ADDITIONAL_INFO,
            DESCRIPTION, RAW_LOG, DEVICE_HASH, SESSION_ID, RISK_SCORE, RISK_LEVEL, CONFIDENCE, CONFIDENCE_LEVEL,
            BASELINE_DEVIATION, ENTROPY, FREQUENCY_ANOMALY, SEQUENCE_ANOMALY, HAS_BEHAVIORAL_ANOMALY,
            SRC_PORT, DST_PORT, PROTOCOL, BYTES, DURATION, CLOUD_SERVICE, RESOURCE_ID, METHOD, MODEL_ID,
            INPUT_HASH, OUTPUT_HASH, MAC_ADDRESS, MESSAGE, RAW, METADATA, ADVANCED_METADATA
    );

    public static final List<String> FILTERABLE_FIELDS = List.of(
            EVENT_ID, TIMESTAMP_YEAR, TIMESTAMP_MONTH, TIMESTAMP_DAY, TIMESTAMP_HOUR,
            TIMESTAMP_MINUTE, TIMESTAMP_SECOND, TIMESTAMP_DATE, TIMESTAMP_DAY_OF_WEEK, TIMESTAMP_IS_WEEKEND,
            SEVERITY, SEVERITY_RANK, EVENT_TYPE, ACTION, EVENT_TYPE_ACTION, USER, HOST, IP, SOURCE, SOURCE_PRODUCT,
            SOURCE_VERSION, GEO_LOCATION, USER_AGENT, USER_AGENT_FAMILY, USER_AGENT_OS, SRC_IP, DST_IP,
            SRC_IP_PREFIX24, DST_IP_PREFIX24, NETWORK_PAIR, ALERT_TYPE, SIGNATURE_ID, CATEGORY, DEVICE_TYPE,
            DEVICE_ID, FIRMWARE_VERSION, OBJECT, PROCESS_ID, PARENT_PROCESS, DEVICE_HASH, SESSION_ID, RISK_SCORE,
            RISK_LEVEL, CONFIDENCE, CONFIDENCE_LEVEL, BASELINE_DEVIATION, ENTROPY, FREQUENCY_ANOMALY,
            SEQUENCE_ANOMALY, HAS_BEHAVIORAL_ANOMALY, SRC_PORT, DST_PORT, PROTOCOL, BYTES, DURATION,
            CLOUD_SERVICE, RESOURCE_ID, METHOD, MODEL_ID, INPUT_HASH, OUTPUT_HASH, MAC_ADDRESS
    );

    public static final List<String> GROUPABLE_FIELDS = List.of(
            TIMESTAMP_YEAR, TIMESTAMP_MONTH, TIMESTAMP_DAY, TIMESTAMP_QUARTER, TIMESTAMP_HOUR, TIMESTAMP_MINUTE, TIMESTAMP_SECOND,
            TIMESTAMP_DATE, TIMESTAMP_DAY_OF_WEEK, TIMESTAMP_IS_WEEKEND, SOURCE, SOURCE_PRODUCT, SOURCE_VERSION,
            SEVERITY, SEVERITY_RANK, EVENT_TYPE, ACTION, EVENT_TYPE_ACTION, USER, HOST, IP, GEO_LOCATION,
            USER_AGENT, USER_AGENT_FAMILY, USER_AGENT_OS, SRC_IP, DST_IP, SRC_IP_PREFIX24, DST_IP_PREFIX24,
            NETWORK_PAIR, ALERT_TYPE, SIGNATURE_ID, CATEGORY, DEVICE_TYPE, DEVICE_ID, FIRMWARE_VERSION, OBJECT,
            PROCESS_ID, PARENT_PROCESS, RISK_LEVEL, CONFIDENCE_LEVEL, FREQUENCY_ANOMALY, SEQUENCE_ANOMALY,
            HAS_BEHAVIORAL_ANOMALY, SRC_PORT, DST_PORT, PROTOCOL, CLOUD_SERVICE, RESOURCE_ID, METHOD,
            MODEL_ID, INPUT_HASH, OUTPUT_HASH, MAC_ADDRESS
    );

    public static final List<String> FULL_TEXT_FIELDS = List.of(MESSAGE, DESCRIPTION, ADDITIONAL_INFO);

    public static final Set<String> NUMERIC_METRIC_FIELDS = Set.of(
            SEVERITY_RANK, PROCESS_ID, RISK_SCORE, CONFIDENCE, BASELINE_DEVIATION, ENTROPY,
            SRC_PORT, DST_PORT, BYTES, DURATION
    );

    public static final List<String> RESPONSE_FIELDS = List.of(
            EVENT_ID, TIMESTAMP, TIMESTAMP_YEAR, TIMESTAMP_MONTH, TIMESTAMP_DAY,
            TIMESTAMP_HOUR, TIMESTAMP_MINUTE, TIMESTAMP_SECOND, TIMESTAMP_DATE, TIMESTAMP_DAY_OF_WEEK,
            TIMESTAMP_IS_WEEKEND, SOURCE, SOURCE_PRODUCT, SOURCE_VERSION, SEVERITY, SEVERITY_RANK, EVENT_TYPE,
            ACTION, EVENT_TYPE_ACTION, USER, HOST, IP, GEO_LOCATION, USER_AGENT, USER_AGENT_FAMILY, USER_AGENT_OS,
            SRC_IP, DST_IP, SRC_IP_PREFIX24, DST_IP_PREFIX24, NETWORK_PAIR, ALERT_TYPE, SIGNATURE_ID, CATEGORY,
            DEVICE_TYPE, DEVICE_ID, FIRMWARE_VERSION, OBJECT, PROCESS_ID, PARENT_PROCESS, ADDITIONAL_INFO,
            DESCRIPTION, RAW_LOG, DEVICE_HASH, SESSION_ID, RISK_SCORE, RISK_LEVEL, CONFIDENCE, CONFIDENCE_LEVEL,
            BASELINE_DEVIATION, ENTROPY, FREQUENCY_ANOMALY, SEQUENCE_ANOMALY, HAS_BEHAVIORAL_ANOMALY,
            SRC_PORT, DST_PORT, PROTOCOL, BYTES, DURATION, CLOUD_SERVICE, RESOURCE_ID, METHOD, MODEL_ID,
            INPUT_HASH, OUTPUT_HASH, MAC_ADDRESS, MESSAGE, RAW, METADATA, ADVANCED_METADATA
    );

    private SocEventSchema() {
    }

    public static String canonicalCsvHeader() {
        return String.join(",", CSV_HEADER_FIELDS);
    }

}
