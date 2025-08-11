package com.sacmauquan.qrordering.dto;

import lombok.*;

@Getter 
@Setter
public class AuthRequest {
    private String email;
    private String password;
}