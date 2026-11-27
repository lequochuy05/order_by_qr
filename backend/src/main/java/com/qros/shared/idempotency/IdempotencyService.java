package com.qros.shared.idempotency;

import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.scheduling.DistributedSchedulerLock;
import com.qros.shared.time.AppTime;
import com.qros.shared.transaction.TransactionExecutor;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
@Lazy(false)
public class IdempotencyService {

    private static final Duration REQUEST_TTL = Duration.ofHours(24);
    private static final String CLEANUP_LOCK_NAME = "idempotency-cleanup";
    private static final Duration CLEANUP_LOCK_TTL = Duration.ofMinutes(10);

    private final IdempotencyRequestRepository repository;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final DistributedSchedulerLock schedulerLock;
    private final TransactionExecutor transactionExecutor;

    public IdempotencyService(
            IdempotencyRequestRepository repository, ObjectMapper objectMapper, MeterRegistry meterRegistry) {
        this(repository, objectMapper, meterRegistry, DistributedSchedulerLock.noOp(), TransactionExecutor.direct());
    }

    @Autowired
    public IdempotencyService(
            IdempotencyRequestRepository repository,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry,
            DistributedSchedulerLock schedulerLock,
            TransactionExecutor transactionExecutor) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
        this.schedulerLock = schedulerLock != null ? schedulerLock : DistributedSchedulerLock.noOp();
        this.transactionExecutor = transactionExecutor != null ? transactionExecutor : TransactionExecutor.direct();
    }

    private Counter claimedCounter;
    private Counter replayedCounter;
    private Counter conflictCounter;

    @PostConstruct
    void initializeMetrics() {
        claimedCounter = meterRegistry.counter("idempotency.requests.claimed");
        replayedCounter = meterRegistry.counter("idempotency.requests.replayed");
        conflictCounter = meterRegistry.counter("idempotency.requests.conflicted");
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public <T> T execute(
            @NonNull String namespace,
            @NonNull String clientRequestKey,
            @NonNull Object requestPayload,
            @NonNull Class<T> responseType,
            @NonNull Supplier<T> action) {
        String normalizedNamespace = normalizeNamespace(namespace);
        String requestKey = sha256(clientRequestKey.trim());
        String requestHash = hashPayload(requestPayload);
        LocalDateTime now = AppTime.now();

        int claimed = repository.claim(normalizedNamespace, requestKey, requestHash, now, now.plus(REQUEST_TTL));

        if (claimed == 0) {
            IdempotencyRequest request = repository
                    .findByNamespaceAndRequestKey(normalizedNamespace, requestKey)
                    .orElseThrow(() -> new IllegalStateException("Unable to load claimed idempotency request"));
            return replayExisting(request, requestHash, responseType);
        }

        claimedCounter.increment();
        T response = action.get();
        repository.complete(
                normalizedNamespace,
                requestKey,
                IdempotencyRequestStatus.SUCCEEDED,
                writeResponse(response),
                AppTime.now());
        return response;
    }

    @Scheduled(cron = "${idempotency.cleanup-cron:0 15 * * * *}")
    public void cleanupExpiredRequests() {
        schedulerLock.runWithLock(
                CLEANUP_LOCK_NAME,
                CLEANUP_LOCK_TTL,
                () -> transactionExecutor.writeWithoutResult(() -> repository.deleteExpired(AppTime.now())));
    }

    private <T> T replayExisting(IdempotencyRequest request, String requestHash, Class<T> responseType) {
        if (!request.getRequestHash().equals(requestHash)) {
            conflictCounter.increment();
            throw new BusinessException(
                    ErrorCode.IDEMPOTENCY_CONFLICT, "Idempotency key is already associated with a different request");
        }

        if (request.getStatus() != IdempotencyRequestStatus.SUCCEEDED || request.getResponseJson() == null) {
            conflictCounter.increment();
            throw new BusinessException(
                    ErrorCode.IDEMPOTENCY_PROCESSING,
                    "This request is still being processed",
                    Map.of("retryAfterSeconds", 1));
        }

        try {
            replayedCounter.increment();
            return objectMapper.readValue(request.getResponseJson(), responseType);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Unable to read stored idempotency response", exception);
        }
    }

    private String hashPayload(Object requestPayload) {
        try {
            return sha256(objectMapper.writeValueAsString(requestPayload));
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Unable to serialize idempotent request", exception);
        }
    }

    private String writeResponse(Object response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Unable to store idempotency response", exception);
        }
    }

    private String normalizeNamespace(String namespace) {
        String normalized = namespace.trim();
        if (normalized.isBlank() || normalized.length() > 100) {
            throw new IllegalArgumentException("Idempotency namespace is invalid");
        }
        return normalized;
    }

    private String sha256(String value) {
        return com.qros.shared.security.HashUtils.sha256Hex(value);
    }
}
