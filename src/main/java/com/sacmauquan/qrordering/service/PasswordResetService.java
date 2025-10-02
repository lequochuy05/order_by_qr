package com.sacmauquan.qrordering.service;

import com.sacmauquan.qrordering.model.PasswordResetToken;
import com.sacmauquan.qrordering.model.User;
import com.sacmauquan.qrordering.repository.PasswordResetTokenRepository;
import com.sacmauquan.qrordering.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PasswordResetService {
    @Autowired private UserRepository userRepo;
    @Autowired private PasswordResetTokenRepository tokenRepo;
    @Autowired private EmailService emailService;
    @Autowired private OtpService otpService;

    // API bằng email
    public void createPasswordResetToken(String email) {
        User user = userRepo.findByEmail(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Email không tồn tại"));

        String token = UUID.randomUUID().toString();
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(30);

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setToken(token);
        resetToken.setExpiryDate(expiry);
        tokenRepo.save(resetToken);

        // Gửi mail
        emailService.sendResetPasswordEmail(email, token);
    }

    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepo.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Token không hợp lệ"));

        if (resetToken.isUsed() || resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token đã hết hạn hoặc đã dùng");
        }

        User user = resetToken.getUser();
        user.setPassword(new BCryptPasswordEncoder().encode(newPassword));
        userRepo.save(user);

        resetToken.setUsed(true);
        tokenRepo.save(resetToken);
    }

    // API bằng SMS
    public void createOtpAndSendOtp(String phone) {
        if (phone.startsWith("+84")) {
            phone = "0" + phone.substring(3);
        }

        User user = userRepo.findByPhone(phone)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Số điện thoại không tồn tại"));

        String otpCode = String.format("%06d", (int)(Math.random() * 1_000_000));
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(5);

        PasswordResetToken otpToken = new PasswordResetToken();
        otpToken.setUser(user);
        otpToken.setOtpCode(otpCode);
        otpToken.setExpiryDate(expiry);
        otpToken.setVia(PasswordResetToken.Via.PHONE);
        otpToken.setToken(UUID.randomUUID().toString()); //  thêm dòng này
        tokenRepo.save(otpToken);

        String message = "Mã OTP của bạn: " + otpCode + " (hết hạn sau 5 phút)";
        otpService.sendOtp(phone, message);
    }

    public void resetPasswordWithOtp(String phone, String otpCode, String newPassword) {
        if (phone.startsWith("+84")) {
            phone = "0" + phone.substring(3);
        }

        PasswordResetToken otpToken = tokenRepo.findValidOtp(otpCode, phone)
            .orElseThrow(() -> new RuntimeException("OTP không hợp lệ hoặc đã hết hạn"));

        User user = otpToken.getUser();
        user.setPassword(new BCryptPasswordEncoder().encode(newPassword));
        userRepo.save(user);

        otpToken.setUsed(true);
        tokenRepo.save(otpToken);
    }

}
