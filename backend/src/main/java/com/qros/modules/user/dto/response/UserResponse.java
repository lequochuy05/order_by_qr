package com.qros.modules.user.dto.response;

import com.qros.modules.user.model.enums.UserRole;
import com.qros.modules.user.model.enums.UserStatus;
import java.time.LocalDateTime;

public record UserResponse (
    Long id,
    String fullName,
    String email,
    String phone,
    UserRole role,
    UserStatus status,
    LocalDateTime createdAt,
    String avatarUrl
){}
