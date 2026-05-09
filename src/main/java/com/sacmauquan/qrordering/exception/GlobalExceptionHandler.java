package com.sacmauquan.qrordering.exception;

import com.sacmauquan.qrordering.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * GlobalExceptionHandler - Quản lý tập trung mọi ngoại lệ của hệ thống.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Xử lý lỗi xác thực dữ liệu đầu vào (@Valid)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage()));
        
        log.warn("Lỗi xác thực dữ liệu: {}", errors);
        return ApiResponse.error("Dữ liệu đầu vào không hợp lệ", errors);
    }

    /**
     * Xử lý ResponseStatusException (Các lỗi ném ra từ tầng Service)
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ApiResponse<Void> handleResponseStatusException(ResponseStatusException ex) {
        return ApiResponse.error(ex.getReason() != null ? ex.getReason() : "Yêu cầu không được thực hiện", null);
    }

    /**
     * Xử lý lỗi không tìm thấy tài nguyên
     */
    @ExceptionHandler(NoSuchElementException.class)
    public ApiResponse<Void> handleNoSuchElementException(NoSuchElementException ex) {
        return ApiResponse.error("Không tìm thấy thông tin yêu cầu", null);
    }

    /**
     * Xử lý lỗi phân quyền (Access Denied)
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ApiResponse<Void> handleAccessDeniedException(AccessDeniedException ex) {
        return ApiResponse.error("Bạn không có quyền thực hiện hành động này", null);
    }

    /**
     * Xử lý lỗi logic nghiệp vụ hoặc trạng thái không hợp lệ
     */
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ApiResponse<Void> handleBusinessLogicException(RuntimeException ex) {
        return ApiResponse.error(ex.getMessage(), null);
    }

    /**
     * Xử lý các lỗi hệ thống không xác định
     */
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleGeneralException(Exception ex) {
        log.error("LỖI HỆ THỐNG: ", ex);
        return ApiResponse.error("Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau.", null);
    }
}