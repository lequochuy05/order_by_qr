package com.qros.modules.table.repository;

import com.qros.modules.table.model.TableSessionToken;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TableSessionTokenRepository extends JpaRepository<TableSessionToken, Long> {

    @EntityGraph(attributePaths = {"session", "session.table"})
    Optional<TableSessionToken> findFirstByTokenHashAndRevokedAtIsNull(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            SELECT t FROM TableSessionToken t
            WHERE t.session.id = :sessionId
              AND t.revokedAt IS NULL
            ORDER BY t.issuedAt ASC
            """)
    List<TableSessionToken> findActiveBySessionIdForUpdate(@Param("sessionId") Long sessionId);

    @Modifying
    @Query(
            """
            UPDATE TableSessionToken t
            SET t.revokedAt = :now
            WHERE t.session.id = :sessionId
              AND t.revokedAt IS NULL
            """)
    int revokeActiveTokensBySessionId(@Param("sessionId") Long sessionId, @Param("now") LocalDateTime now);
}
