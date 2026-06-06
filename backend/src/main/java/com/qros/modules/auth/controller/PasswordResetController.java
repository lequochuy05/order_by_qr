package com.qros.modules.auth.controller;

import com.qros.shared.response.ApiResponse;
import com.qros.modules.auth.dto.PasswordResetRequest;
import com.qros.modules.auth.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

/**
 * PasswordResetController - Handles the password recovery and reset processes.
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService resetService;

    /**
     * Requests a password reset via email.
     * Always returns a generic success message to prevent email enumeration
     * attacks.
     * 
     * @param email The user's email address
     * @return Void success response
     */
    @PostMapping("/forgot-password-email")
    public ApiResponse<Void> forgotPassword(@RequestParam @NonNull String email) {
        try {
            resetService.createPasswordResetToken(email);
        } catch (Exception e) {
            log.warn("Password reset requested for non-existent or error email: {}", email);
        }
        return ApiResponse.success(
                "If your email exists in our system, you will receive password reset instructions.", null);
    }

    /**
     * Confirms and resets the password using a verification token.
     * The new password is provided in the request body for security.
     * 
     * @param req Request containing the reset token and new password
     * @return Void success response
     */
    @PostMapping("/reset-password-email")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody @NonNull PasswordResetRequest req) {
        resetService.resetPassword(req.getToken(), req.getNewPassword());
        return ApiResponse.success("Password reset successfully.", null);
    }

    /**
     * Requests an OTP code for password recovery via phone number.
     * 
     * @param phone The user's phone number
     * @return Void success response
     */
    @PostMapping("/forgot-password-phone")
    public ApiResponse<Void> forgotPasswordPhone(@RequestParam @NonNull String phone) {
        try {
            resetService.createOtpAndSendOtp(phone);
        } catch (Exception e) {
            log.warn("OTP requested for non-existent or error phone number: {}", phone);
        }
        return ApiResponse.success("If the phone number exists, an OTP code has been sent.", null);
    }

    /**
     * Confirms and resets the password using an OTP code and phone number.
     * 
     * @param req Request containing the phone number, OTP, and new password
     * @return Void success response
     */
    @PostMapping("/reset-password-phone")
    public ApiResponse<Void> resetPasswordPhone(@Valid @RequestBody @NonNull PasswordResetRequest req) {
        resetService.resetPasswordWithOtp(req.getPhone(), req.getOtp(), req.getNewPassword());
        return ApiResponse.success("Password reset successfully.", null);
    }
}
