package com.qros.modules.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record StaffResetPasswordRequest(
        @NotBlank(message = "New password is required")
                @Pattern(
                        regexp = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%^&*(),.?\":{}|<>]).{8,}$",
                        message =
                                "Password must be at least 8 characters, include 1 uppercase letter, 1 number, and 1 special character")
                String newPassword) {}
