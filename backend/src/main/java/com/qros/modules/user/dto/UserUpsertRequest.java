package com.qros.modules.user.dto;

import com.qros.modules.user.model.User;
import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * UserUpsertRequest - Data transfer object for creating or updating a user account.
 */
@Data
public class UserUpsertRequest {
    /**
     * Full name of the user.
     */
    @NotBlank(message = "Full name cannot be empty")
    @Size(min = 2, max = 50, message = "Full name must be between 2 and 50 characters")
    private String fullName;

    /**
     * Email address for the account. Must be unique.
     */
    @NotBlank(message = "Email cannot be empty")
    @Email(message = "Email format is invalid")
    private String email;

    /**
     * Account password. Required for creation, optional for updates.
     */
    @Size(min = 6, message = "Password must be at least 6 characters long")
    private String password;

    /**
     * Contact phone number. Must follow Vietnamese phone format.
     */
    @Pattern(regexp = "^(84|0)[0-9]{9}\\b", message = "Phone number is invalid")
    private String phone;

    /**
     * Role assigned to the user (e.g., MANAGER, STAFF, CHEF).
     */
    private User.Role role;

    /**
     * Current status of the user account (e.g., ACTIVE, INACTIVE).
     */
    private String status;
}