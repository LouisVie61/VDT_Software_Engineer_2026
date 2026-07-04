package vdt.se.demo.domain.exception;

import tools.jackson.databind.JsonNode;

public class BadQueryException extends RuntimeException {
    private final String reasonCode;
    private final JsonNode generatedDsl;

    public BadQueryException(String message) {
        this("BAD_QUERY", message);
    }

    public BadQueryException(String reasonCode, String message) {
        super(message);
        this.reasonCode = reasonCode;
        this.generatedDsl = null;
    }

    public BadQueryException(String reasonCode, String message, JsonNode generatedDsl) {
        super(message);
        this.reasonCode = reasonCode;
        this.generatedDsl = generatedDsl;
    }

    public BadQueryException(String message, Throwable cause) {
        super(message, cause);
        this.reasonCode = "BAD_QUERY";
        this.generatedDsl = null;
    }

    public String getReasonCode() { return reasonCode; }
    public JsonNode getGeneratedDsl() { return generatedDsl; }
}
