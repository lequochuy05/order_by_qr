package com.sacmauquan.qrordering.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * PasswordResetRequest - DTO cho luồng đặt lại mật khẩu.
 * Hỗ trợ cả Token (Email) và OTP (Phone).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetRequest {
    
    // Dùng cho reset bằng Email
    private String token;
    
    // Dùng cho reset bằng Phone
    private String phone;
    private String otp;
    
    // Mật khẩu mới (Bắt buộc cho cả 2 luồng)
    @NotBlank(message = "Mật khẩu mới không được để trống")
    private String newPassword;
}
