package com.sacmauquan.qrordering.dto;

import com.sacmauquan.qrordering.model.User.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {
    private String message;
    private Long userId;
    private String fullName;
    private Role role;
}
