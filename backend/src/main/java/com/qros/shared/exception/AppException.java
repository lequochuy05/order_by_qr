package com.qros.shared.exception;

import java.util.Map;
import java.util.Objects;

/**
 * Application-wide runtime exception that carries a specific error code.
 * This is the base class for most application-specific errors.
 */
public class AppException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ErrorCode errorCode;
    private final Map<String, Object> details;

    public AppException(ErrorCode errorCode) {
        this(errorCode, errorCode.getDefaultMessage(), null, null);
    }

    public AppException(ErrorCode errorCode, String message) {
        this(errorCode, message, null, null);
    }

    public AppException(ErrorCode errorCode, String message, Map<String, Object> details) {
        this(errorCode, message, details, null);
    }

    public AppException(ErrorCode errorCode, String message, Throwable cause) {
        this(errorCode, message, null, cause);
    }

    public AppException(ErrorCode errorCode, String message, Map<String, Object> details, Throwable cause) {
        super(resolveMessage(errorCode, message), cause);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
        this.details = details == null ? Map.of() : Map.copyOf(details);
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    private static String resolveMessage(ErrorCode errorCode, String message) {
        Objects.requireNonNull(errorCode, "errorCode must not be null");
        return message == null || message.isBlank() ? errorCode.getDefaultMessage() : message;
    }
}
