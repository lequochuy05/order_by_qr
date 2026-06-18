package com.qros.modules.user.dto.request;

import jakarta.validation.constraints.*;

public record ProfileUpdateRequest(
        @NotBlank(message = "Full name cannot be empty")
                @Size(min = 2, max = 50, message = "Full name must be between 2 and 50 characters")
                String fullName,
        @Pattern(regexp = "^(84|0)[0-9]{9}\\b", message = "Phone number is invalid") String phone) {}
