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
 * PasswordResetService - Quản lý quy trình khôi phục mật khẩu qua Email và SMS.
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
     * Khôi phục qua Email
     */
    @Transactional
    public void createPasswordResetToken(String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Email không tồn tại"));

        String token = UUID.randomUUID().toString();

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setToken(token);
        resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(30));
        resetToken.setVia(PasswordResetToken.Via.EMAIL);

        tokenRepo.save(resetToken);

        emailService.sendResetPasswordEmail(Objects.requireNonNull(email), Objects.requireNonNull(token));
        log.info("Reset password link sent to email: {}", email);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepo.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Token không hợp lệ hoặc đã hết hạn"));

        if (resetToken.isUsed() || resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token đã hết hạn hoặc đã được sử dụng");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepo.save(user);

        resetToken.setUsed(true);
        tokenRepo.save(resetToken);

        log.info("Password reset successfully for user: {}", user.getEmail());
    }

    /**
     * Khôi phục qua SMS (OTP)
     */
    @Transactional
    public void createOtpAndSendOtp(String phone) {
        String normalizedPhone = normalizePhone(phone);

        User user = userRepo.findByPhone(normalizedPhone)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Số điện thoại không tồn tại"));

        String otpCode = String.format("%06d", (int) (Math.random() * 1_000_000));

        PasswordResetToken otpToken = new PasswordResetToken();
        otpToken.setUser(user);
        otpToken.setOtpCode(otpCode);
        otpToken.setExpiryDate(LocalDateTime.now().plusMinutes(5));
        otpToken.setVia(PasswordResetToken.Via.PHONE);
        otpToken.setToken(UUID.randomUUID().toString());

        tokenRepo.save(otpToken);

        String message = "Mã OTP của bạn là: " + otpCode + ". Hiệu lực trong 5 phút.";
        otpService.sendOtp(normalizedPhone, message);
        log.info("OTP sent to phone: {}", normalizedPhone);
    }

    @Transactional
    public void resetPasswordWithOtp(String phone, String otpCode, String newPassword) {
        String normalizedPhone = normalizePhone(phone);

        PasswordResetToken otpToken = tokenRepo.findValidOtp(otpCode, normalizedPhone)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Mã OTP không hợp lệ hoặc đã hết hạn"));

        User user = otpToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepo.save(user);

        otpToken.setUsed(true);
        tokenRepo.save(otpToken);

        log.info("Password reset successfully via OTP for phone: {}", normalizedPhone);
    }

    /**
     * Chuẩn hóa số điện thoại về định dạng 0xxx
     */
    private String normalizePhone(String phone) {
        Objects.requireNonNull(phone, "Số điện thoại không được để trống");
        if (phone.startsWith("+84")) {
            return "0" + phone.substring(3);
        }
        return phone;
    }
}
