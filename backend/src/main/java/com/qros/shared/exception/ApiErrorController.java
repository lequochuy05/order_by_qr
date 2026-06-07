package com.qros.shared.exception;

import com.qros.shared.response.ApiResponse;
import com.qros.shared.response.ErrorResponse;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ApiErrorController implements ErrorController {

    @RequestMapping("/error")
    public ResponseEntity<ApiResponse<ErrorResponse>> handleError(HttpServletRequest request) {
        HttpStatus status = resolveStatus(request);
        ErrorCode errorCode = resolveErrorCode(status);
        String message = errorCode == ErrorCode.RESOURCE_NOT_FOUND
                ? "Requested resource not found"
                : errorCode.getDefaultMessage();

        ErrorResponse error = ErrorResponse.builder()
                .code(errorCode.name())
                .message(message)
                .details(Map.of())
                .build();

        return ResponseEntity.status(status)
                .body(ApiResponse.error(status.value(), message, error));
    }

    private HttpStatus resolveStatus(HttpServletRequest request) {
        Object statusCode = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (statusCode instanceof Integer code) {
            return HttpStatus.resolve(code) != null ? HttpStatus.resolve(code) : HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private ErrorCode resolveErrorCode(HttpStatus status) {
        return switch (status) {
            case UNAUTHORIZED -> ErrorCode.UNAUTHORIZED;
            case FORBIDDEN -> ErrorCode.FORBIDDEN;
            case NOT_FOUND -> ErrorCode.RESOURCE_NOT_FOUND;
            case CONFLICT -> ErrorCode.CONFLICT;
            default -> status.is4xxClientError() ? ErrorCode.INVALID_REQUEST : ErrorCode.APP_ERROR;
        };
    }
}
