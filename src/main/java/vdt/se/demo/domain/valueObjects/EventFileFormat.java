package vdt.se.demo.domain.valueObjects;

public enum EventFileFormat {
    JSONL("jsonl", ".jsonl"),
    CSV("csv", ".csv");

    private final String directoryName;
    private final String extension;

    EventFileFormat(String directoryName, String extension) {
        this.directoryName = directoryName;
        this.extension = extension;
    }

    public String directoryName() {
        return directoryName;
    }

    public String extension() {
        return extension;
    }
}
