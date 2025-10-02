package com.sacmauquan.qrordering.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sacmauquan.qrordering.model.PasswordResetToken;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);
    Optional<PasswordResetToken> findByOtpCodeAndUser_Phone(String otpCode, String phone);

    @Query("SELECT t FROM PasswordResetToken t WHERE t.otpCode = :otp AND t.user.phone = :phone AND t.used = false AND t.expiryDate > CURRENT_TIMESTAMP")
    Optional<PasswordResetToken> findValidOtp(@Param("otp") String otp, @Param("phone") String phone);


}
