package com.sacmauquan.qrordering.dto;

import com.sacmauquan.qrordering.model.User;
import lombok.*;
import java.time.LocalDateTime;

/**
 * UserResponse - DTO an toàn trả về thông tin người dùng cho Frontend
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private User.Role role;
    private String status;
    private LocalDateTime createdAt;
    private String avatarUrl;
}
