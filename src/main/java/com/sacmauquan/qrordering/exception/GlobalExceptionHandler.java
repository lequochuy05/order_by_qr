package com.sacmauquan.qrordering.exception;

import com.sacmauquan.qrordering.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.handler.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

/**
 * GlobalExceptionHandler - Centralized management of system exceptions.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle validation errors (@Valid)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Object> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        log.warn("Validation error: {}", errors);
        return ApiResponse.error("Invalid input data", errors);
    }

    /**
     * Handle ResponseStatusException (Errors thrown from Service layer)
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Object>> handleResponseStatusException(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode())
                .body(ApiResponse.error(ex.getReason() != null ? ex.getReason() : "Request could not be processed",
                        null));
    }

    /**
     * Handle resource not found errors
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ApiResponse<Object> handleNoResourceFound(NoResourceFoundException ex) {
        return ApiResponse.error("Requested resource not found", null);
    }

    /**
     * Handle access denied errors
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ApiResponse<Object> handleAccessDenied(AccessDeniedException ex) {
        return ApiResponse.error("You do not have permission to perform this action", null);
    }

    /**
     * Handle business logic errors or invalid states
     */
    @ExceptionHandler(IllegalStateException.class)
    public ApiResponse<Object> handleIllegalState(IllegalStateException ex) {
        return ApiResponse.error(ex.getMessage(), null);
    }

    /**
     * Handle unknown system errors
     */
    @ExceptionHandler(Exception.class)
    public ApiResponse<Object> handleGlobalException(Exception ex) {
        log.error("System error: ", ex);
        return ApiResponse.error("Internal server error. Please try again later.", null);
    }
}