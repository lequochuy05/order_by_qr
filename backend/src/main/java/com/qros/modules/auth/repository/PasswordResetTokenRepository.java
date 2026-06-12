package com.qros.modules.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;

import com.qros.modules.auth.model.PasswordResetToken;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
        Optional<PasswordResetToken> findByToken(String token);

        Optional<PasswordResetToken> findFirstByUserPhoneAndUsedFalseAndExpiryDateAfterAndViaOrderByExpiryDateDesc(
                        String phone,
                        LocalDateTime now,
                        @Param("via") PasswordResetToken.Via via);

        @Modifying
        @Query("""
                        UPDATE PasswordResetToken t
                        SET t.used = true
                        WHERE t.user.id = :userId AND t.used = false
                        """)
        void markAllActiveTokensUsedByUserId(@Param("userId") Long userId);
}
