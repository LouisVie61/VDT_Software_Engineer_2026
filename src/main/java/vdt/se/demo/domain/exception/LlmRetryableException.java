package vdt.se.demo.domain.exception;

public class LlmRetryableException extends LlmException {
    public LlmRetryableException(String message) {
        super(message);
    }

    public LlmRetryableException(String message, Throwable cause) {
        super(message, cause);
    }
}