package com.sacmauquan.qrordering.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.Collection;
import java.util.Collections;

/**
 * User - Entity representing a system user (Staff, Manager, or Chef).
 * Implements Spring Security's UserDetails for authentication and authorization.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@SQLDelete(sql = "UPDATE users SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class User extends BaseEntity implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique email address used for login and notifications.
     */
    @NotBlank(message = "Email cannot be empty")
    @Email(message = "Email format is invalid")
    @Column(length = 50, unique = true, nullable = false)
    private String email;

    /**
     * URL of the user's profile picture stored on a cloud provider.
     */
    @Column(length = 150)
    private String avatarUrl;

    /**
     * Full legal name of the user.
     */
    @NotBlank(message = "Full name cannot be empty")
    @Column(length = 50, nullable = false)
    private String fullName;

    /**
     * Hashed password for account security.
     */
    @Column(length = 100, nullable = false)
    @JsonIgnore
    private String password;

    /**
     * Contact phone number.
     */
    @Column(unique = true, length = 15)
    private String phone;

    /**
     * Assigned security role within the application.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    /**
     * Current account operational status.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() {
        return email != null ? email : phone;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status != UserStatus.BANNED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return !isDeleted() && status == UserStatus.ACTIVE;
    }

    /**
     * Enum for application security roles.
     */
    public enum Role {
        STAFF, MANAGER, CHEF
    }

    /**
     * Enum for user account statuses.
     */
    public enum UserStatus {
        ACTIVE, BANNED, INACTIVE
    }
}