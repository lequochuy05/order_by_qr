package com.qros.shared.response;

import java.time.Instant;

/**
 * ApiResponse - Standardized API response structure for the entire system.
 * Ensures consistent data delivery, status reporting, and error handling.
 *
 * @param <T> Type of the data payload
 */
public class ApiResponse<T> {

    /**
     * Timestamp of when the response was generated.
     */
    private Instant timestamp = Instant.now();

    /**
     * Indicates if the operation was successful.
     */
    private boolean success;

    /**
     * HTTP-like status code for the operation.
     */
    private int status;

    /**
     * Descriptive message about the operation result.
     */
    private String message;

    /**
     * Data payload of the response.
     */
    private T data;

    public ApiResponse() {}

    public ApiResponse(Instant timestamp, boolean success, int status, String message, T data) {
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.success = success;
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public boolean isSuccess() {
        return success;
    }

    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setData(T data) {
        this.data = data;
    }

    /**
     * Creates a successful response with data and a default success message.
     *
     * @param data The data payload
     * @return ApiResponse instance
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .status(200)
                .message("Success")
                .data(data)
                .build();
    }

    /**
     * Creates a successful response with a custom message and data.
     *
     * @param message Custom success message
     * @param data    The data payload
     * @return ApiResponse instance
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .status(200)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(int status, String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .status(status)
                .message(message)
                .data(data)
                .build();
    }

    /**
     * Creates an error response with a specific status code and message.
     *
     * @param status  HTTP-like error status code
     * @param message Descriptive error message
     * @return ApiResponse instance
     */
    public static <T> ApiResponse<T> error(int status, String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .status(status)
                .message(message)
                .build();
    }

    public static <T> ApiResponse<T> error(int status, String message, T data) {
        return ApiResponse.<T>builder()
                .success(false)
                .status(status)
                .message(message)
                .data(data)
                .build();
    }

    /**
     * Creates an error response with a custom message and error data (e.g.,
     * validation details).
     *
     * @param message Descriptive error message
     * @param data    Error data payload
     * @return ApiResponse instance
     */
    public static <T> ApiResponse<T> error(String message, T data) {
        return ApiResponse.<T>builder()
                .success(false)
                .status(500)
                .message(message)
                .data(data)
                .build();
    }

    /**
     * Creates an error response with a custom message and default 500 status.
     *
     * @param message Descriptive error message
     * @return ApiResponse instance
     */
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .status(500)
                .message(message)
                .build();
    }

    public static <T> ApiResponseBuilder<T> builder() {
        return new ApiResponseBuilder<>();
    }

    public static class ApiResponseBuilder<T> {
        private Instant timestamp = Instant.now();
        private boolean success;
        private int status;
        private String message;
        private T data;

        public ApiResponseBuilder<T> timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public ApiResponseBuilder<T> success(boolean success) {
            this.success = success;
            return this;
        }

        public ApiResponseBuilder<T> status(int status) {
            this.status = status;
            return this;
        }

        public ApiResponseBuilder<T> message(String message) {
            this.message = message;
            return this;
        }

        public ApiResponseBuilder<T> data(T data) {
            this.data = data;
            return this;
        }

        public ApiResponse<T> build() {
            ApiResponse<T> resp = new ApiResponse<>();
            resp.timestamp = this.timestamp;
            resp.success = this.success;
            resp.status = this.status;
            resp.message = this.message;
            resp.data = this.data;
            return resp;
        }
    }
}
