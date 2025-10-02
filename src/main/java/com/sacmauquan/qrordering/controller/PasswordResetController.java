package com.sacmauquan.qrordering.controller;

import com.sacmauquan.qrordering.service.PasswordResetService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/auth")
public class PasswordResetController {
    @Autowired private PasswordResetService resetService;

    // API bằng email
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestParam String email) {
        resetService.createPasswordResetToken(email);
        return ResponseEntity.ok("Đã gửi email đặt lại mật khẩu");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestParam String token,
                                           @RequestParam String newPassword) {
        resetService.resetPassword(token, newPassword);
        return ResponseEntity.ok("Đặt lại mật khẩu thành công");
    }

    // API bằng SMS
    @PostMapping("/forgot-password-phone")
    public ResponseEntity<?> forgotPasswordPhone(@RequestParam String phone) {
        resetService.createOtpAndSendOtp(phone);
        return ResponseEntity.ok("Đã gửi OTP về số điện thoại");
    }

    @PostMapping("/reset-password-phone")
    public ResponseEntity<?> resetPasswordPhone(@RequestParam String phone,
                                                @RequestParam String otp,
                                                @RequestParam String newPassword) {
        resetService.resetPasswordWithOtp(phone, otp, newPassword);
        return ResponseEntity.ok("Đặt lại mật khẩu thành công");
    }
}
