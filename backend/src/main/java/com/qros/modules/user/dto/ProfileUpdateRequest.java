package com.qros.modules.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * ProfileUpdateRequest - Safe self-service profile update payload.
 */
@Data
public class ProfileUpdateRequest {
    @NotBlank(message = "Full name cannot be empty")
    @Size(min = 2, max = 50, message = "Full name must be between 2 and 50 characters")
    private String fullName;

    @Pattern(regexp = "^(84|0)[0-9]{9}\\b", message = "Phone number is invalid")
    private String phone;
}
