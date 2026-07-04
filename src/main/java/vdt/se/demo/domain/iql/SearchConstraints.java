package vdt.se.demo.domain.iql;

public record SearchConstraints(String from, String to, String severity, String eventType,
                                String user, String host, String ip) {
    public static SearchConstraints empty() { return new SearchConstraints(null, null, null, null, null, null, null); }
}
