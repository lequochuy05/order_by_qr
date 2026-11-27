package com.qros.shared.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Immutable, cache-safe principal snapshot used by JWT and WebSocket authentication.
 * Intentionally omits password hashes and derives GrantedAuthority on demand so
 * Caffeine and Redis JSON serialization remain safe and compact.
 */
public record CachedUserPrincipal(
        Long id,
        String email,
        String roleName,
        boolean enabled,
        boolean accountNonLocked,
        LocalDateTime passwordChangedAt,
        boolean mustChangePassword)
        implements UserDetails, PasswordVersionedUserDetails {

    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (roleName == null || roleName.isBlank()) {
            return Collections.emptyList();
        }
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + roleName));
    }

    @Override
    @JsonIgnore
    public String getPassword() {
        return "";
    }

    @Override
    @JsonIgnore
    public String getUsername() {
        return email;
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    @JsonIgnore
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public LocalDateTime getPasswordChangedAt() {
        return passwordChangedAt;
    }

    @Override
    public boolean isMustChangePassword() {
        return mustChangePassword;
    }
}
