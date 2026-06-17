package com.qros.modules.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * LoginRequest - Data transfer object for login requests.
 */
public record LoginRequest(
        @NotBlank(message = "Email cannot be empty") @Email(message = "Email is invalid") String email,
        @NotBlank(message = "Password cannot be empty") String password) {}
