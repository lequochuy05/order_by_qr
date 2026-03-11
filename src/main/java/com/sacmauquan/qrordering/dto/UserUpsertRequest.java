package com.sacmauquan.qrordering.dto;

import com.sacmauquan.qrordering.model.User;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UserUpsertRequest {
    @NotBlank(message = "Họ tên không được để trống")
    @Size(min = 2, max = 50, message = "Họ tên phải từ 2 đến 50 ký tự")
    private String fullName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
    private String password;

    @Pattern(regexp = "^(84|0)[0-9]{9}\\b", message = "Số điện thoại không hợp lệ")
    private String phone;

    private User.Role role;
    private String status;
}