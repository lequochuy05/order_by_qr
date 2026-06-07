package com.qros.shared.exception;

import com.qros.shared.response.ApiResponse;
import com.qros.shared.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * GlobalExceptionHandler - Centralized management of system exceptions.
 * Provides consistent error responses across the entire API.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleAppException(AppException ex) {
        ErrorCode errorCode = ex.getErrorCode();

        log.warn("Application error [{}]: {}", errorCode.name(), ex.getMessage());
        return buildErrorResponse(errorCode, ex.getMessage(), ex.getDetails());
    }

    /**
     * Handles validation errors triggered by @Valid annotations on DTOs.
     * 
     * @param ex MethodArgumentNotValidException containing validation results
     * @return ApiResponse containing field-specific error messages
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, Object> errors = new LinkedHashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String errorMessage = error.getDefaultMessage();
            if (error instanceof FieldError fieldError) {
                errors.put(fieldError.getField(), errorMessage);
            } else if (error instanceof ObjectError objectError) {
                errors.put(objectError.getObjectName(), errorMessage);
            }
        });
        log.warn("Validation error: {}", errors);
        return buildErrorResponse(ErrorCode.VALIDATION_ERROR, ErrorCode.VALIDATION_ERROR.getDefaultMessage(), errors);
    }

    /**
     * Handles 404 errors when a requested URL or static resource is not found.
     * 
     * @param ex NoResourceFoundException
     * @return ResponseEntity with 404 Not Found status
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleNoResourceFound(NoResourceFoundException ex) {
        return buildErrorResponse(ErrorCode.RESOURCE_NOT_FOUND, "Requested resource not found", Map.of());
    }

    /**
     * Handles authorization errors when a user lacks the required role or permission.
     * 
     * @param ex AccessDeniedException
     * @return ResponseEntity with 403 Forbidden status
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleAccessDenied(AccessDeniedException ex) {
        return buildErrorResponse(ErrorCode.FORBIDDEN, "You do not have permission to perform this action", Map.of());
    }

    /**
     * Handles IllegalStateException for invalid business logic transitions or states.
     * 
     * @param ex IllegalStateException
     * @return ResponseEntity with 400 Bad Request status
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleIllegalState(IllegalStateException ex) {
        return buildErrorResponse(ErrorCode.ORDER_INVALID_STATE, ex.getMessage(), Map.of());
    }

    /**
     * Catch-all handler for any unhandled exceptions to prevent leaking internal details.
     * 
     * @param ex The unhandled exception
     * @return ResponseEntity with 500 Internal Server Error status
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleGlobalException(Exception ex) {
        log.error("System error: ", ex);
        return buildErrorResponse(ErrorCode.APP_ERROR, "Internal server error. Please try again later.", Map.of());
    }

    private ResponseEntity<ApiResponse<ErrorResponse>> buildErrorResponse(
            ErrorCode errorCode,
            String message,
            Map<String, Object> details) {
        return buildErrorResponse(errorCode.getStatus(), errorCode.name(),
                resolveMessage(errorCode.getDefaultMessage(), message), details);
    }

    private ResponseEntity<ApiResponse<ErrorResponse>> buildErrorResponse(
            HttpStatusCode status,
            String code,
            String message,
            Map<String, Object> details) {
        ErrorResponse error = ErrorResponse.builder()
                .code(code)
                .message(message)
                .details(details == null ? Map.of() : details)
                .build();
        return ResponseEntity.status(status)
                .body(ApiResponse.error(status.value(), message, error));
    }

    private String resolveMessage(String defaultMessage, String message) {
        return message == null || message.isBlank() ? defaultMessage : message;
    }
}
