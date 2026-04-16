package com.sacmauquan.qrordering.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgumentException(IllegalArgumentException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage() != null ? ex.getMessage() : "Dữ liệu không hợp lệ");
        problemDetail.setTitle("Bad Request");
        problemDetail.setType(java.util.Objects.requireNonNull(URI.create("https://api.qr-ordering.com/errors/bad-request")));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ProblemDetail handleNoSuchElementException(NoSuchElementException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage() != null ? ex.getMessage() : "Không tìm thấy dữ liệu");
        problemDetail.setTitle("Resource Not Found");
        problemDetail.setType(java.util.Objects.requireNonNull(URI.create("https://api.qr-ordering.com/errors/not-found")));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalStateException(IllegalStateException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage() != null ? ex.getMessage() : "Trạng thái không hợp lệ");
        problemDetail.setTitle("Conflict / Illegal State");
        problemDetail.setType(java.util.Objects.requireNonNull(URI.create("https://api.qr-ordering.com/errors/conflict")));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage()));
                
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Lỗi xác thực dữ liệu đầu vào");
        problemDetail.setTitle("Validation Error");
        problemDetail.setType(java.util.Objects.requireNonNull(URI.create("https://api.qr-ordering.com/errors/validation")));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("errors", errors);
        return problemDetail;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneralException(Exception ex) {
        ex.printStackTrace(); // Keep this for server logs
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage() != null ? ex.getMessage() : "Lỗi hệ thống nội bộ");
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setType(java.util.Objects.requireNonNull(URI.create("https://api.qr-ordering.com/errors/internal-error")));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }
}