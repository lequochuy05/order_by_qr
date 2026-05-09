package com.sacmauquan.qrordering.model;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "password_reset_tokens")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE password_reset_tokens SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class PasswordResetToken extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Người dùng không được để trống")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotBlank(message = "Token không được để trống")
    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    @Builder.Default
    @Column(nullable = false)
    private boolean used = false;

    @Column(length = 6)
    private String otpCode;

    @NotNull(message = "Kênh gửi không được để trống")
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Via via;

    public enum Via {
        EMAIL, PHONE
    }
}
