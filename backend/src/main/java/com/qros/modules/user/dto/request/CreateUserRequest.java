package com.qros.modules.user.dto.request;

import com.qros.modules.user.model.enums.UserRole;
import com.qros.modules.user.model.enums.UserStatus;
import jakarta.validation.constraints.*;

public record CreateUserRequest(
        @NotBlank(message = "Full name is required") String fullName,
        @NotBlank(message = "Email is required") @Email(message = "Invalid email format") String email,
        @Pattern(regexp = "^(84|0)[0-9]{9}\\b", message = "Phone number is invalid") String phone,
        @NotBlank(message = "Password is required") @Size(min = 6) String password,
        @NotNull UserRole role,
        @NotNull UserStatus status) {}
