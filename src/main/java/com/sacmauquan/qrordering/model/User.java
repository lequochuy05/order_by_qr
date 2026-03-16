package com.sacmauquan.qrordering.model;

import jakarta.persistence.*;
import lombok.*; // Import all lombok
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor // Bắt buộc cho JPA
@AllArgsConstructor // Bắt buộc để dùng Builder
@Builder // Giúp tạo object nhanh gọn trong Service
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String password;

    @Column(unique = true)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // --- NÂNG CẤP: Dùng Enum cho Status ---
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.ACTIVE;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    // ===== ENUMS
    public enum Role {
        STAFF, MANAGER, CHEF
    }

    public enum Status {
        ACTIVE,
        BANNED,
        INACTIVE
    }
}