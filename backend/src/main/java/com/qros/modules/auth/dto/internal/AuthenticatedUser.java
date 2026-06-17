package com.qros.modules.auth.dto.internal;

import com.qros.modules.user.model.User;
import com.qros.modules.user.model.enums.UserRole;

public record AuthenticatedUser(Long userId, String email, UserRole role) {
    public static AuthenticatedUser from(User user) {
        return new AuthenticatedUser(user.getId(), user.getEmail(), user.getRole());
    }
}
