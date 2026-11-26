package com.qros.shared.idempotency;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IdempotencyRequestRepository extends JpaRepository<IdempotencyRequest, Long> {

    Optional<IdempotencyRequest> findByNamespaceAndRequestKey(String namespace, String requestKey);

    default int claim(
            String namespace, String requestKey, String requestHash, LocalDateTime createdAt, LocalDateTime expiresAt) {
        return claimWithStatus(
                namespace, requestKey, requestHash, IdempotencyRequestStatus.PROCESSING.name(), createdAt, expiresAt);
    }

    @Modifying
    @Query(value = """
                    INSERT INTO idempotency_requests (
                        namespace,
                        request_key,
                        request_hash,
                        status,
                        created_at,
                        expires_at
                    )
                    VALUES (
                        :namespace,
                        :requestKey,
                        :requestHash,
                        :status,
                        :createdAt,
                        :expiresAt
                    )
                    ON CONFLICT (namespace, request_key) DO UPDATE
                    SET request_hash = EXCLUDED.request_hash,
                        status = EXCLUDED.status,
                        response_json = NULL,
                        created_at = EXCLUDED.created_at,
                        completed_at = NULL,
                        expires_at = EXCLUDED.expires_at
                    WHERE idempotency_requests.expires_at <= EXCLUDED.created_at
                    """, nativeQuery = true)
    int claimWithStatus(
            @Param("namespace") String namespace,
            @Param("requestKey") String requestKey,
            @Param("requestHash") String requestHash,
            @Param("status") String status,
            @Param("createdAt") LocalDateTime createdAt,
            @Param("expiresAt") LocalDateTime expiresAt);

    @Modifying
    @Query("""
            UPDATE IdempotencyRequest request
            SET request.status = :status,
                request.responseJson = :responseJson,
                request.completedAt = :completedAt
            WHERE request.namespace = :namespace
              AND request.requestKey = :requestKey
            """)
    int complete(
            @Param("namespace") String namespace,
            @Param("requestKey") String requestKey,
            @Param("status") IdempotencyRequestStatus status,
            @Param("responseJson") String responseJson,
            @Param("completedAt") LocalDateTime completedAt);

    @Modifying
    @Query("DELETE FROM IdempotencyRequest request WHERE request.expiresAt < :cutoff")
    int deleteExpired(@Param("cutoff") LocalDateTime cutoff);
}
