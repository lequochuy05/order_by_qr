package com.qros.shared.exception;

import com.qros.shared.response.ApiResponse;
import com.qros.shared.response.ErrorResponse;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, Object> errors = new LinkedHashMap<>();
        ex.getConstraintViolations()
                .forEach(violation -> errors.put(violation.getPropertyPath().toString(), violation.getMessage()));
        log.warn("Constraint violation: {}", errors);
        return buildErrorResponse(ErrorCode.VALIDATION_ERROR, ErrorCode.VALIDATION_ERROR.getDefaultMessage(), errors);
    }

    @ExceptionHandler({OptimisticLockException.class, ObjectOptimisticLockingFailureException.class})
    public ResponseEntity<ApiResponse<ErrorResponse>> handleOptimisticLock(Exception ex) {
        log.warn("Concurrent modification detected: {}", ex.getMessage());
        return buildErrorResponse(
                ErrorCode.CONCURRENT_MODIFICATION,
                "This resource was changed by another request. Please refresh and try again.",
                Map.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return buildErrorResponse(
                ErrorCode.DATA_INTEGRITY_VIOLATION, "The request conflicts with existing or related data.", Map.of());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleUnreadableMessage(HttpMessageNotReadableException ex) {
        log.warn("Malformed request body: {}", ex.getMessage());
        return buildErrorResponse(
                ErrorCode.MALFORMED_REQUEST, "Request body is malformed or contains an invalid value.", Map.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return buildErrorResponse(
                ErrorCode.INVALID_REQUEST,
                "Invalid value for parameter: " + ex.getName(),
                Map.of("parameter", ex.getName()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleMissingRequestParameter(
            MissingServletRequestParameterException ex) {
        return buildErrorResponse(
                ErrorCode.INVALID_REQUEST,
                "Missing required parameter: " + ex.getParameterName(),
                Map.of("parameter", ex.getParameterName()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex) {
        return buildErrorResponse(
                ErrorCode.INVALID_REQUEST, "HTTP method is not supported for this endpoint.", Map.of());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex) {
        return buildErrorResponse(
                ErrorCode.INVALID_REQUEST, "Content type is not supported for this endpoint.", Map.of());
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
            ErrorCode errorCode, String message, Map<String, Object> details) {
        return buildErrorResponse(
                errorCode.getStatus(),
                errorCode.name(),
                resolveMessage(errorCode.getDefaultMessage(), message),
                details);
    }

    private ResponseEntity<ApiResponse<ErrorResponse>> buildErrorResponse(
            HttpStatusCode status, String code, String message, Map<String, Object> details) {
        ErrorResponse error = ErrorResponse.builder()
                .code(code)
                .message(message)
                .details(details == null ? Map.of() : details)
                .build();
        ResponseEntity.BodyBuilder response = ResponseEntity.status(status);
        Object retryAfterSeconds = details == null ? null : details.get("retryAfterSeconds");
        if (retryAfterSeconds != null) {
            response.header(HttpHeaders.RETRY_AFTER, retryAfterSeconds.toString());
        }
        return response.body(ApiResponse.error(status.value(), message, error));
    }

    private String resolveMessage(String defaultMessage, String message) {
        return message == null || message.isBlank() ? defaultMessage : message;
    }
}
