package com.qros.modules.user.dto.request;

import com.qros.modules.user.model.enums.UserRole;
import com.qros.modules.user.model.enums.UserStatus;
import jakarta.validation.constraints.*;

public record CreateUserRequest(
        @NotBlank(message = "Full name is required") String fullName,
        @NotBlank(message = "Email is required") @Email(message = "Invalid email format") String email,
        @Pattern(regexp = "^(84|0)[0-9]{9}\\b", message = "Phone number is invalid") String phone,
        @NotBlank(message = "Password is required")
                @Pattern(
                        regexp = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%^&*(),.?\":{}|<>]).{8,}$",
                        message =
                                "Password must be at least 8 characters, include 1 uppercase letter, 1 number, and 1 special character")
                String password,
        @NotNull UserRole role,
        @NotNull UserStatus status) {}
