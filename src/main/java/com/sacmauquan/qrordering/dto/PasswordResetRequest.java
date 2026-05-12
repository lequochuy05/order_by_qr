package com.sacmauquan.qrordering.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * PasswordResetRequest - Data transfer object for the password reset flow.
 * Supports verification via Token (Email) or OTP (Phone).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetRequest {
    
    /**
     * Verification token sent via email.
     */
    private String token;
    
    /**
     * User's phone number for OTP-based reset.
     */
    private String phone;

    /**
     * OTP code sent via SMS for phone-based reset.
     */
    private String otp;
    
    /**
     * The new password to be set for the account.
     */
    @NotBlank(message = "New password cannot be empty")
    private String newPassword;
}
