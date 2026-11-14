package com.qros.shared.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class IdempotencyServiceTest {

    @Test
    void storesSuccessfulResponseAndRunsActionOnce() {
        IdempotencyRequestRepository repository = mock(IdempotencyRequestRepository.class);
        when(repository.claim(anyString(), anyString(), anyString(), any(), any()))
                .thenReturn(1);

        IdempotencyService service = new IdempotencyService(repository, new ObjectMapper(), new SimpleMeterRegistry());
        service.initializeMetrics();
        AtomicInteger executions = new AtomicInteger();

        TestResponse response =
                service.execute("public-order", "request-key", new TestRequest("A"), TestResponse.class, () -> {
                    executions.incrementAndGet();
                    return new TestResponse("created");
                });

        assertThat(response.value()).isEqualTo("created");
        assertThat(executions).hasValue(1);
        verify(repository, never()).findByNamespaceAndRequestKey(anyString(), anyString());
        verify(repository)
                .complete(
                        anyString(),
                        anyString(),
                        org.mockito.ArgumentMatchers.eq(IdempotencyRequestStatus.SUCCEEDED),
                        org.mockito.ArgumentMatchers.contains("created"),
                        any());
    }

    @Test
    void rejectsSameKeyWithDifferentPayload() {
        IdempotencyRequestRepository repository = mock(IdempotencyRequestRepository.class);
        IdempotencyRequest request = processingRequest("different-hash");
        when(repository.claim(anyString(), anyString(), anyString(), any(), any()))
                .thenReturn(0);
        when(repository.findByNamespaceAndRequestKey(anyString(), anyString())).thenReturn(Optional.of(request));

        IdempotencyService service = new IdempotencyService(repository, new ObjectMapper(), new SimpleMeterRegistry());
        service.initializeMetrics();

        assertThatThrownBy(() -> service.execute(
                        "public-order",
                        "request-key",
                        new TestRequest("A"),
                        TestResponse.class,
                        () -> new TestResponse("created")))
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.IDEMPOTENCY_CONFLICT));
        verify(repository, never()).save(any());
    }

    private IdempotencyRequest processingRequest(String hash) {
        IdempotencyRequest request = new IdempotencyRequest();
        request.setNamespace("public-order");
        request.setRequestKey("key");
        request.setRequestHash(hash);
        request.setStatus(IdempotencyRequestStatus.PROCESSING);
        request.setCreatedAt(LocalDateTime.now());
        request.setExpiresAt(LocalDateTime.now().plusHours(1));
        return request;
    }

    private record TestRequest(String value) {}

    private record TestResponse(String value) {}
}
