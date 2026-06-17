package com.qros.modules.auth.controller;

import com.qros.modules.auth.dto.request.EmailPasswordResetRequest;
import com.qros.modules.auth.dto.request.PhonePasswordResetRequest;
import com.qros.modules.auth.service.PasswordResetService;
import com.qros.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

/**
 * PasswordResetController - Handles the password recovery and reset processes.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService resetService;

    @PostMapping("/forgot-password-email")
    public ApiResponse<Void> forgotPassword(@RequestParam @NonNull String email) {
        try {
            resetService.createPasswordResetToken(email);
        } catch (Exception e) {
            // log.warn("Password reset requested for non-existent or error email: {}", email);
        }
        return ApiResponse.success(
                "If your email exists in our system, you will receive password reset instructions.", null);
    }

    @PostMapping("/reset-password-email")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody @NonNull EmailPasswordResetRequest req) {
        resetService.resetPassword(req.token(), req.newPassword());
        return ApiResponse.success("Password reset successfully.", null);
    }

    @PostMapping("/forgot-password-phone")
    public ApiResponse<Void> forgotPasswordPhone(@RequestParam @NonNull String phone) {
        try {
            resetService.createOtpAndSendOtp(phone);
        } catch (Exception e) {
            // log.warn("OTP requested for non-existent or error phone number: {}", phone);
        }
        return ApiResponse.success("If the phone number exists, an OTP code has been sent.", null);
    }

    @PostMapping("/reset-password-phone")
    public ApiResponse<Void> resetPasswordPhone(@Valid @RequestBody @NonNull PhonePasswordResetRequest req) {
        resetService.resetPasswordWithOtp(req.phone(), req.otp(), req.newPassword());
        return ApiResponse.success("Password reset successfully.", null);
    }
}
