// src/main/java/com/sacmauquan/qrordering/model/User.java
package com.sacmauquan.qrordering.model;

import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity @Table(name="users")
@Getter @Setter
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="full_name")
    private String fullName;

    private String phone;

    @Column(unique = true, nullable = false)
    private String email;

    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    // NEW: ACTIVE/INACTIVE
    @Column(nullable = false)
    private String status = "ACTIVE";

    // NEW: để sort/hiển thị nếu cần
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    public enum Role { 
        STAFF, MANAGER 
    }
}
