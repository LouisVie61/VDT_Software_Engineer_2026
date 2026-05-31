package vdt.se.demo.adapter.out.elasticsearch;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Document(indexName = "soc-events", createIndex = false)
public class SocEventDocument {
    @Id
    private String id;
    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private Instant timestamp;
    @Field(type = FieldType.Keyword)
    private String source;
    @Field(type = FieldType.Keyword)
    private String severity;
    @Field(name = "event_type", type = FieldType.Keyword)
    private String eventType;
    @Field(type = FieldType.Keyword)
    private String user;
    @Field(type = FieldType.Keyword)
    private String host;
    @Field(type = FieldType.Ip)
    private String ip;
    @Field(name = "src_ip", type = FieldType.Ip)
    private String srcIp;
    @Field(name = "dst_ip", type = FieldType.Ip)
    private String dstIp;
    @Field(type = FieldType.Keyword)
    private String action;
    @Field(type = FieldType.Text)
    private String message;
    @Field(type = FieldType.Text)
    private String raw;
    @Field(type = FieldType.Object)
    private Map<String, Object> metadata = new LinkedHashMap<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getSrcIp() {
        return srcIp;
    }

    public void setSrcIp(String srcIp) {
        this.srcIp = srcIp;
    }

    public String getDstIp() {
        return dstIp;
    }

    public void setDstIp(String dstIp) {
        this.dstIp = dstIp;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRaw() {
        return raw;
    }

    public void setRaw(String raw) {
        this.raw = raw;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
    }
}
