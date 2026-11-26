package com.qros.modules.user.dto.request;

import com.qros.modules.user.model.enums.UserRole;
import com.qros.modules.user.model.enums.UserStatus;
import com.qros.shared.validation.MaxUtf8Bytes;
import jakarta.validation.constraints.*;

public record CreateUserRequest(
        @NotBlank(message = "Full name is required") @Size(max = 100, message = "Full name must not exceed 100 characters") String fullName,

        @NotBlank(message = "Email is required") @Email(message = "Invalid email format") @Size(max = 254, message = "Email must not exceed 254 characters") String email,

        @Size(max = 15, message = "Phone number must not exceed 15 characters") @Pattern(regexp = "^(84|0)[0-9]{9}$", message = "Phone number is invalid")
        String phone,

        @NotBlank(message = "Password is required") @Size(max = 72, message = "Password must not exceed 72 characters") @MaxUtf8Bytes(value = 72, message = "Password must not exceed 72 UTF-8 bytes")
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9\\s]).{8,72}$",
                message =
                        "Password must be 8-72 characters, include 1 uppercase letter, 1 number, and 1 special character")
        String password,

        @NotNull UserRole role,
        @NotNull UserStatus status) {}
