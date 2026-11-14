package com.qros.shared.response;

import java.time.Instant;
import java.util.Map;

public class ErrorResponse {
    private Instant timestamp = Instant.now();
    private String code;
    private String message;
    private Map<String, Object> details = Map.of();

    public ErrorResponse() {}

    public ErrorResponse(Instant timestamp, String code, String message, Map<String, Object> details) {
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.code = code;
        this.message = message;
        this.details = details != null ? details : Map.of();
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details != null ? details : Map.of();
    }

    public static ErrorResponseBuilder builder() {
        return new ErrorResponseBuilder();
    }

    public static class ErrorResponseBuilder {
        private Instant timestamp = Instant.now();
        private String code;
        private String message;
        private Map<String, Object> details = Map.of();

        public ErrorResponseBuilder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public ErrorResponseBuilder code(String code) {
            this.code = code;
            return this;
        }

        public ErrorResponseBuilder message(String message) {
            this.message = message;
            return this;
        }

        public ErrorResponseBuilder details(Map<String, Object> details) {
            this.details = details != null ? details : Map.of();
            return this;
        }

        public ErrorResponse build() {
            return new ErrorResponse(timestamp, code, message, details);
        }
    }
}
