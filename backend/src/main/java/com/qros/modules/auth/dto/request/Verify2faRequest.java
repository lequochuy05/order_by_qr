package com.qros.modules.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record Verify2faRequest(
        @NotBlank(message = "Pre-auth token cannot be blank") String preAuthToken,
        @NotBlank(message = "OTP cannot be blank")
                @Pattern(regexp = "^[0-9]{6}$", message = "Mã OTP phải là dãy 6 chữ số")
                String otp) {}
