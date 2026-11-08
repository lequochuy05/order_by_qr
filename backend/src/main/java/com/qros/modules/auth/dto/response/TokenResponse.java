package com.qros.modules.auth.dto.response;

import com.qros.modules.user.model.User;
import com.qros.modules.user.model.enums.UserRole;

public record TokenResponse(
        String accessToken,
        Long userId,
        String fullName,
        UserRole role,
        String avatarUrl,
        String email,
        boolean mustChangePassword) {

    public TokenResponse(
            String accessToken, Long userId, String fullName, UserRole role, String avatarUrl, String email) {
        this(accessToken, userId, fullName, role, avatarUrl, email, false);
    }

    public static TokenResponse of(User user, String accessToken) {
        return new TokenResponse(
                accessToken,
                user.getId(),
                user.getFullName(),
                user.getRole(),
                user.getAvatarUrl(),
                user.getEmail(),
                user.isMustChangePassword());
    }
}
