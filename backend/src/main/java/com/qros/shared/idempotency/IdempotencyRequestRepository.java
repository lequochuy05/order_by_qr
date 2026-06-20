package com.qros.shared.idempotency;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IdempotencyRequestRepository extends JpaRepository<IdempotencyRequest, Long> {

    Optional<IdempotencyRequest> findByNamespaceAndRequestKey(String namespace, String requestKey);

    @Modifying
    @Query(
            value =
                    """
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
                        'PROCESSING',
                        :createdAt,
                        :expiresAt
                    )
                    ON CONFLICT (namespace, request_key) DO NOTHING
                    """,
            nativeQuery = true)
    int claim(
            @Param("namespace") String namespace,
            @Param("requestKey") String requestKey,
            @Param("requestHash") String requestHash,
            @Param("createdAt") LocalDateTime createdAt,
            @Param("expiresAt") LocalDateTime expiresAt);

    @Modifying
    @Query("DELETE FROM IdempotencyRequest request WHERE request.expiresAt < :cutoff")
    int deleteExpired(@Param("cutoff") LocalDateTime cutoff);
}
