package com.sacmauquan.qrordering.model;

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
 * Stores either a secure token for email-based reset or an OTP for phone-based reset.
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

    /**
     * The user who requested the password reset.
     */
    @NotNull(message = "User cannot be empty")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Secure unique token (for email) or identifier for the reset request.
     */
    @NotBlank(message = "Token cannot be empty")
    @Column(nullable = false, unique = true)
    private String token;

    /**
     * Timestamp when the token or OTP will expire.
     */
    @Column(nullable = false)
    private LocalDateTime expiryDate;

    /**
     * Flag indicating if this reset request has already been successfully used.
     */
    @Builder.Default
    @Column(nullable = false)
    private boolean used = false;

    /**
     * Short numeric code sent via SMS for phone-based recovery.
     */
    @Column(length = 6)
    private String otpCode;

    /**
     * The communication channel used for the reset request (EMAIL or PHONE).
     */
    @NotNull(message = "Delivery channel cannot be empty")
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Via via;

    /**
     * Enum for password reset delivery channels.
     */
    public enum Via {
        EMAIL, PHONE
    }
}
