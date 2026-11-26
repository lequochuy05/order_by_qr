package com.qros.modules.user.dto.request;

import com.qros.shared.validation.MaxUtf8Bytes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record StaffResetPasswordRequest(
        @NotBlank(message = "New password is required") @Size(max = 72, message = "Password must not exceed 72 characters") @MaxUtf8Bytes(value = 72, message = "Password must not exceed 72 UTF-8 bytes")
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9\\s]).{8,72}$",
                message =
                        "Password must be 8-72 characters, include 1 uppercase letter, 1 number, and 1 special character")
        String newPassword) {}
