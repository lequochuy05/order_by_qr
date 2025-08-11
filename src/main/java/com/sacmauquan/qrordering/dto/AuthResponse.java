package com.sacmauquan.qrordering.dto;

import com.sacmauquan.qrordering.model.User;

import lombok.*;

@Getter 
@AllArgsConstructor
public class AuthResponse {
    private Long userId;
    private String fullName;
    private User.Role role;
    private String accessToken;
}