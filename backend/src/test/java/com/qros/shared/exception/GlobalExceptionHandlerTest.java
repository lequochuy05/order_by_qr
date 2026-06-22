package com.qros.shared.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.qros.shared.response.ApiResponse;
import com.qros.shared.response.ErrorResponse;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsOptimisticLockToConflictWithoutLeakingInternalDetails() {
        ResponseEntity<ApiResponse<ErrorResponse>> response =
                handler.handleOptimisticLock(new OptimisticLockException("database details"));

        assertError(
                response,
                409,
                ErrorCode.CONCURRENT_MODIFICATION,
                "This resource was changed by another request. Please refresh and try again.");
    }

    @Test
    void mapsDataIntegrityViolationToConflict() {
        ResponseEntity<ApiResponse<ErrorResponse>> response =
                handler.handleDataIntegrityViolation(new DataIntegrityViolationException("constraint details"));

        assertError(
                response,
                409,
                ErrorCode.DATA_INTEGRITY_VIOLATION,
                "The request conflicts with existing or related data.");
    }

    @Test
    void mapsUnreadableRequestBodyToBadRequest() {
        ResponseEntity<ApiResponse<ErrorResponse>> response =
                handler.handleUnreadableMessage(new HttpMessageNotReadableException("invalid json"));

        assertError(
                response, 400, ErrorCode.MALFORMED_REQUEST, "Request body is malformed or contains an invalid value.");
    }

    private void assertError(
            ResponseEntity<ApiResponse<ErrorResponse>> response,
            int expectedStatus,
            ErrorCode expectedCode,
            String expectedMessage) {
        assertThat(response.getStatusCode().value()).isEqualTo(expectedStatus);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).isEqualTo(expectedMessage);
        assertThat(response.getBody().getData().getCode()).isEqualTo(expectedCode.name());
        assertThat(response.getBody().getData().getMessage()).isEqualTo(expectedMessage);
    }
}
