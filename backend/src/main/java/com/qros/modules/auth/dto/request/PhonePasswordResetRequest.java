package com.qros.modules.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PhonePasswordResetRequest(

    @NotBlank(message = "Phone number is required")
    String phone,

    @NotBlank(message = "OTP is required")
    String otp,

    @NotBlank(message = "New password is required")
    @Size(min = 6, message = "Password must be at least 6 characters long")
    String newPassword
) {  }
