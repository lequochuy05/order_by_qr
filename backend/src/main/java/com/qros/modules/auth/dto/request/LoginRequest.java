package com.qros.modules.auth.dto.request;

import com.qros.shared.validation.MaxUtf8Bytes;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * LoginRequest - Data transfer object for login requests.
 */
public record LoginRequest(
        @NotBlank(message = "Email cannot be empty") @Email(message = "Email is invalid") @Size(max = 254, message = "Email must not exceed 254 characters") String email,

        @NotBlank(message = "Password cannot be empty") @Size(max = 72, message = "Password must not exceed 72 characters") @MaxUtf8Bytes(value = 72, message = "Password must not exceed 72 UTF-8 bytes")
        String password) {}
