// dto/UserDto.java  (trả về cho list/get)
package com.sacmauquan.qrordering.dto;
import com.sacmauquan.qrordering.model.User;
import lombok.*;
import java.time.Instant;

@Getter
@Builder
public class UserDto {
    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private User.Role role;
    private String status;
    private Instant createdAt;
    private String avatarUrl;
}