package com.qros.modules.auth.repository;

import com.qros.modules.auth.model.PasswordResetToken;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM PasswordResetToken t WHERE t.token = :token")
    Optional<PasswordResetToken> findByTokenForUpdate(@Param("token") String token);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            SELECT t FROM PasswordResetToken t
            WHERE t.user.phone = :phone
              AND t.used = false
              AND t.expiryDate > :now
              AND t.via = :via
            ORDER BY t.expiryDate DESC
            """)
    List<PasswordResetToken> findLatestOtpForUpdate(
            @Param("phone") String phone,
            @Param("now") LocalDateTime now,
            @Param("via") PasswordResetToken.Via via,
            Pageable pageable);

    @Modifying
    @Query(
            """
                        UPDATE PasswordResetToken t
                        SET t.used = true
                        WHERE t.user.id = :userId AND t.used = false
                        """)
    void markAllActiveTokensUsedByUserId(@Param("userId") Long userId);
}
