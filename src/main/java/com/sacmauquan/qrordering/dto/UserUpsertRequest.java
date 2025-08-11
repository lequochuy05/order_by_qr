package com.sacmauquan.qrordering.dto;

import com.sacmauquan.qrordering.model.User;
import lombok.*;

@Getter 
@Setter
public class UserUpsertRequest {
    private String fullName;
    private String email;      // tạo mới/đăng ký dùng; update có thể bỏ qua nếu không đổi
    private String phone;
    private String password;   // optional khi update; bắt buộc khi register/tạo mới
    private User.Role role;    // null => mặc định STAFF
    private String status;     // "ACTIVE" | "INACTIVE" ; null => ACTIVE
}