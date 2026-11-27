package com.qros.modules.user.dto.request;

import com.qros.modules.user.model.enums.UserRole;
import com.qros.modules.user.model.enums.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @NotBlank(message = "Full name is required") @Size(max = 100, message = "Full name must not exceed 100 characters") String fullName,

        @NotBlank(message = "Email is required") @Email(message = "Invalid email format") @Size(max = 254, message = "Email must not exceed 254 characters") String email,

        @Size(max = 15, message = "Phone number must not exceed 15 characters") @Pattern(regexp = "^(84|0)[0-9]{9}$", message = "Phone number is invalid")
        String phone,

        UserRole role,
        UserStatus status) {}
