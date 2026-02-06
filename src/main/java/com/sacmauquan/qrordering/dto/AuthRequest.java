package com.sacmauquan.qrordering.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
public class AuthRequest {
    @NotBlank(message = "Email không được để trống")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    private String password;
}