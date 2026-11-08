package com.qros.modules.auth.dto.response;

import com.qros.modules.user.model.User;
import com.qros.modules.user.model.enums.UserRole;

public record LoginResponse(
        Long userId,
        String fullName,
        UserRole role,
        String accessToken,
        String avatarUrl,
        String email,
        boolean mustChangePassword,
        boolean requires2fa,
        String preAuthToken) {

    public LoginResponse(
            Long userId, String fullName, UserRole role, String accessToken, String avatarUrl, String email) {
        this(userId, fullName, role, accessToken, avatarUrl, email, false, false, null);
    }

    public LoginResponse(
            Long userId,
            String fullName,
            UserRole role,
            String accessToken,
            String avatarUrl,
            String email,
            boolean mustChangePassword) {
        this(userId, fullName, role, accessToken, avatarUrl, email, mustChangePassword, false, null);
    }

    public static LoginResponse of(User user, String accessToken) {
        return new LoginResponse(
                user.getId(),
                user.getFullName(),
                user.getRole(),
                accessToken,
                user.getAvatarUrl(),
                user.getEmail(),
                user.isMustChangePassword(),
                false,
                null);
    }

    public static LoginResponse require2fa(String email, String preAuthToken) {
        return new LoginResponse(null, null, UserRole.ADMIN, null, null, email, false, true, preAuthToken);
    }
}
