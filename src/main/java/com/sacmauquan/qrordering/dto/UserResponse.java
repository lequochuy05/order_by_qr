package com.sacmauquan.qrordering.dto;

import java.time.Instant;

import com.sacmauquan.qrordering.model.User;

import lombok.*;

@AllArgsConstructor 
@Getter 
@Setter
public class UserResponse {
  private Long id;
  private String fullName;
  private String email;
  private String phone;
  private User.Role role;
  private String status;
  private Instant createdAt;
}
