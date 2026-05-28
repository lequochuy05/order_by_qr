package com.sacmauquan.qrordering.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * AuthRequest - Data transfer object for login requests.
 */
@Data
public class AuthRequest {
    /**
     * User's registered email address.
     */
    @NotBlank(message = "Email cannot be empty")
    private String email;

    /**
     * User's account password.
     */
    @NotBlank(message = "Password cannot be empty")
    private String password;
}