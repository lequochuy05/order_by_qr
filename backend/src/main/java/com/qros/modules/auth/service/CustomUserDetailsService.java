package com.qros.modules.auth.service;

import com.qros.modules.user.model.User;
import com.qros.modules.user.service.UserLookupService;
import com.qros.shared.cache.CacheNames;
import com.qros.shared.security.CachedUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

/**
 * CustomUserDetailsService - Implementation of Spring Security's UserDetailsService.
 * Caches an immutable, password-free {@link CachedUserPrincipal} snapshot keyed by normalized email
 * so JWT and WebSocket authentication do not hit PostgreSQL on every request.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserLookupService userRepository;

    @Override
    @Cacheable(
            value = CacheNames.USER_AUTH,
            key = "#email == null ? '' : #email.trim().toLowerCase()",
            unless = "#result == null")
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        if (email == null || email.isBlank()) {
            throw new UsernameNotFoundException("User email must not be blank");
        }
        String normalizedEmail = email.trim().toLowerCase();
        User user = userRepository
                .findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        return new CachedUserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getRole() != null ? user.getRole().name() : null,
                user.isEnabled(),
                user.isAccountNonLocked(),
                user.getPasswordChangedAt(),
                user.isMustChangePassword());
    }
}
