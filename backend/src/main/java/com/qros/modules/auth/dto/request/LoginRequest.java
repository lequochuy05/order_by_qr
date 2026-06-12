package com.qros.modules.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;

/**
 * LoginRequest - Data transfer object for login requests.
 */

public record LoginRequest(

        @NotBlank(message = "Email cannot be empty")
        @Email(message = "Email is invalid")
        String email,

        @NotBlank(message = "Password cannot be empty")
        String password

) {
}