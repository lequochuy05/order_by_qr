// dto/UserDto.java  
package com.sacmauquan.qrordering.dto;
import com.sacmauquan.qrordering.model.User;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private User.Role role;
    private String status;
    private LocalDateTime createdAt;
    private String avatarUrl;
}