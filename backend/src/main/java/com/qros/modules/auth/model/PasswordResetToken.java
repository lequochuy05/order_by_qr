package com.qros.modules.auth.model;

import com.qros.shared.entity.BaseEntity;
import com.qros.modules.user.model.User;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/**
 * PasswordResetToken - Entity for managing password recovery requests.
 * Stores either a secure token for email-based reset or an OTP for phone-based
 * reset.
 */
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

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotBlank
    @Column(nullable = false, unique = true, length = 64)
    private String token;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime expiryDate;

    @Builder.Default
    @Column(nullable = false)
    private boolean used = false;

    @Column(length = 64)
    private String otpCode;

    @NotNull
    @Builder.Default
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Via via = Via.EMAIL;

    public enum Via {
        EMAIL, PHONE
    }
}
