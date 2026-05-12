package com.sacmauquan.qrordering.service;

import com.sacmauquan.qrordering.model.PasswordResetToken;
import com.sacmauquan.qrordering.model.User;
import com.sacmauquan.qrordering.repository.PasswordResetTokenRepository;
import com.sacmauquan.qrordering.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * PasswordResetService - Manages secure password recovery flows via Email (token-based) and SMS (OTP-based).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepo;
    private final PasswordResetTokenRepository tokenRepo;
    private final EmailService emailService;
    private final OtpService otpService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Initiates a password reset flow by generating a unique token and sending it via email.
     * 
     * @param email The registered email address
     */
    @Transactional
    public void createPasswordResetToken(String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Email not found"));

        String token = UUID.randomUUID().toString();

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setToken(token);
        resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(30));
        resetToken.setVia(PasswordResetToken.Via.EMAIL);

        tokenRepo.save(resetToken);

        emailService.sendResetPasswordEmail(Objects.requireNonNull(email), Objects.requireNonNull(token));
        log.info("Password reset link dispatched to email: {}", email);
    }

    /**
     * Completes the password reset process using a valid security token.
     * 
     * @param token The reset token from email
     * @param newPassword The new plain text password
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepo.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Token invalid or expired"));

        if (resetToken.isUsed() || resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token expired or already used");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepo.save(user);

        resetToken.setUsed(true);
        tokenRepo.save(resetToken);

        log.info("Password successfully reset via email token for user: {}", user.getEmail());
    }

    /**
     * Initiates a mobile-based password reset by generating and sending a 6-digit OTP.
     * 
     * @param phone The registered phone number
     */
    @Transactional
    public void createOtpAndSendOtp(String phone) {
        String normalizedPhone = normalizePhone(phone);

        User user = userRepo.findByPhone(normalizedPhone)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Phone number not found"));

        String otpCode = String.format("%06d", (int) (Math.random() * 1_000_000));

        PasswordResetToken otpToken = new PasswordResetToken();
        otpToken.setUser(user);
        otpToken.setOtpCode(otpCode);
        otpToken.setExpiryDate(LocalDateTime.now().plusMinutes(5));
        otpToken.setVia(PasswordResetToken.Via.PHONE);
        otpToken.setToken(UUID.randomUUID().toString());

        tokenRepo.save(otpToken);

        String message = "Your Sắc Màu Quán OTP is: " + otpCode + ". Valid for 5 minutes.";
        otpService.sendOtp(normalizedPhone, message);
        log.info("OTP successfully dispatched to phone: {}", normalizedPhone);
    }

    /**
     * Completes the password reset process using a valid OTP code.
     * 
     * @param phone The registered phone number
     * @param otpCode The 6-digit OTP received via SMS
     * @param newPassword The new plain text password
     */
    @Transactional
    public void resetPasswordWithOtp(String phone, String otpCode, String newPassword) {
        String normalizedPhone = normalizePhone(phone);

        PasswordResetToken otpToken = tokenRepo.findValidOtp(otpCode, normalizedPhone)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "OTP invalid or expired"));

        User user = otpToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepo.save(user);

        otpToken.setUsed(true);
        tokenRepo.save(otpToken);

        log.info("Password successfully reset via SMS OTP for phone: {}", normalizedPhone);
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
}
