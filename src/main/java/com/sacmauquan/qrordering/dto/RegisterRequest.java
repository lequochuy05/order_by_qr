package com.sacmauquan.qrordering.dto;

import com.sacmauquan.qrordering.model.User.Role;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {
    private String fullName;
    private String phone;
    private String email;
    private String password;
    private Role role;
}
