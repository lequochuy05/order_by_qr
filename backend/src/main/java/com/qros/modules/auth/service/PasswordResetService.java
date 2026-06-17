package com.qros.modules.auth.service;

import com.qros.infrastructure.mail.SmtpEmailService;
import com.qros.modules.auth.model.PasswordResetToken;
import com.qros.modules.user.model.User;
import com.qros.modules.auth.repository.PasswordResetTokenRepository;
import com.qros.modules.user.repository.UserRepository;
import com.qros.shared.transaction.TransactionSideEffectService;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.time.AppTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.security.SecureRandom;
import java.util.UUID;

/**
 * PasswordResetService - Manages secure password recovery flows via Email
 * (token-based) and SMS (OTP-based).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepo;
    private final PasswordResetTokenRepository tokenRepo;
    private final SmtpEmailService emailService;

    private final PasswordEncoder passwordEncoder;
    private final TransactionSideEffectService sideEffects;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Initiates a password reset flow by generating a unique token and sending it
     * via email.
     * 
     * @param email The registered email address
     */
    @Transactional
    public void createPasswordResetToken(String email) {
        String normalizedEmail = email.trim().toLowerCase();
        
        User user = userRepo.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.EMAIL_NOT_FOUND));

        String rawToken = UUID.randomUUID().toString();

        PasswordResetToken resetToken = new PasswordResetToken();
        tokenRepo.markAllActiveTokensUsedByUserId(user.getId());

        resetToken.setUser(user);
        resetToken.setToken(sha256(rawToken));
        resetToken.setExpiryDate(AppTime.now().plusMinutes(30));
        resetToken.setVia(PasswordResetToken.Via.EMAIL);
        resetToken.setUsed(false);

        tokenRepo.save(resetToken);

        sideEffects.afterCommit(
                () -> emailService.sendResetPasswordEmail(normalizedEmail, rawToken),
                "send reset password email");
    }

    /**
     * Completes the password reset process using a valid security token.
     * 
     * @param token  The reset token from email
     * @param newPassword The new plain text password
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepo.findByToken(sha256(token))
                .orElseThrow(() -> new BusinessException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID,
                        "Token invalid or expired"));

        if (resetToken.isUsed() || resetToken.getExpiryDate().isBefore(AppTime.now())) {
            throw new BusinessException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID, "Token expired or already used");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepo.save(user);

        resetToken.setUsed(true);
        tokenRepo.save(resetToken);

    }

    /**
     * Initiates a mobile-based password reset by generating and sending a 6-digit
     * OTP.
     * 
     * @param phone The registered phone number
     */
    @Transactional
    public void createOtpAndSendOtp(String phone) {
        String normalizedPhone = normalizePhone(phone);

        User user = userRepo.findByPhone(normalizedPhone)
                .orElseThrow(() -> new BusinessException(ErrorCode.PHONE_NOT_FOUND));
        tokenRepo.markAllActiveTokensUsedByUserId(user.getId());

        String rawOtpCode = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));

        PasswordResetToken otpToken = new PasswordResetToken();
        otpToken.setUser(user);
        otpToken.setOtpCode(sha256(rawOtpCode));
        otpToken.setToken(sha256(UUID.randomUUID().toString()));
        otpToken.setExpiryDate(AppTime.now().plusMinutes(5));
        otpToken.setVia(PasswordResetToken.Via.PHONE);
        otpToken.setUsed(false);

        tokenRepo.save(otpToken);

        // TODO: Integrate actual SMS gateway (e.g. Twilio, AWS SNS) in production.
        // For now, print to console to simulate sending.
        log.warn("=========================================================");
        log.warn("📱 SMS GATEWAY NOT CONFIGURED");
        log.warn("📱 SIMULATED SMS TO {}: 'Your QROS password reset OTP is {}'", normalizedPhone, rawOtpCode);
        log.warn("=========================================================");
    }

    /**
     * Completes the password reset process using a valid OTP code.
     * 
     * @param phone  The registered phone number
     * @param otpCode     The 6-digit OTP received via SMS
     * @param newPassword The new plain text password
     */
    @Transactional
    public void resetPasswordWithOtp(String phone, String otpCode, String newPassword) {
        String normalizedPhone = normalizePhone(phone);

        PasswordResetToken otpToken = tokenRepo.findFirstByUserPhoneAndUsedFalseAndExpiryDateAfterAndViaOrderByExpiryDateDesc(
                normalizedPhone, AppTime.now(), PasswordResetToken.Via.PHONE)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.PASSWORD_RESET_TOKEN_INVALID,
                        "OTP invalid or expired"));

        if (!otpToken.getOtpCode().equals(sha256(otpCode))) {
            throw new BusinessException(
                    ErrorCode.PASSWORD_RESET_TOKEN_INVALID,
                    "OTP invalid or expired");
        }

        User user = otpToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepo.save(user);

        otpToken.setUsed(true);
        tokenRepo.save(otpToken);
    }

    /**
     * Normalizes Vietnamese phone number formats (e.g., converting +84 to 0).
     */
    private String normalizePhone(String phone) {
        Objects.requireNonNull(phone, "Phone number cannot be null");
        if (phone.startsWith("+84")) {
            return "0" + phone.substring(3);
        }
        return phone;
    }

    /**
     * Hashes a string using SHA-256.
     */
    private String sha256(String raw) {
        try {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot hash token", e);
        }
    }
}
