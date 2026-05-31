package vdt.se.demo.domain.exception;

public class BadQueryException extends RuntimeException {
    public BadQueryException(String message) {
        super(message);
    }

    public BadQueryException(String message, Throwable cause) {
        super(message, cause);
    }
}