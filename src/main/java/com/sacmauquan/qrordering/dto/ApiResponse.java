package com.sacmauquan.qrordering.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

/**
 * ApiResponse - Standardized API response structure for the entire system.
 * Ensures consistent data delivery, status reporting, and error handling.
 * 
 * @param <T> Type of the data payload
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    /**
     * Timestamp of when the response was generated.
     */
    @Builder.Default
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
}
