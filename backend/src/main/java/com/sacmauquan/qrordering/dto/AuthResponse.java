package com.sacmauquan.qrordering.dto;

import com.sacmauquan.qrordering.model.User;

import lombok.*;

/**
 * AuthResponse - Data transfer object returned after a successful login.
 */
@Getter
@AllArgsConstructor
public class AuthResponse {
    /**
     * Unique identifier of the logged-in user.
     */
    private Long userId;

    /**
     * Full name of the user.
     */
    private String fullName;
    private User.Role role;
    private String accessToken;
    private String avatarUrl;
    private String email;
}
