package com.qros.modules.auth.dto.response;

import com.qros.modules.user.model.User;
import com.qros.modules.user.model.enums.UserRole;

public record LoginResponse(
        Long userId, String fullName, UserRole role, String accessToken, String avatarUrl, String email) {
    public static LoginResponse of(User user, String accessToken) {
        return new LoginResponse(
                user.getId(), user.getFullName(), user.getRole(), accessToken, user.getAvatarUrl(), user.getEmail());
    }
}
