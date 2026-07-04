package vdt.se.demo.domain.exception;

public final class LlmRateLimitException extends LlmRetryableException {
    public LlmRateLimitException(String message) { super(message); }
    public LlmRateLimitException(String message, Throwable cause) { super(message, cause); }
}
