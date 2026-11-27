package com.qros.modules.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qros.modules.user.listener.UserCacheInvalidationListener;
import com.qros.modules.user.model.User;
import com.qros.modules.user.model.enums.UserRole;
import com.qros.modules.user.model.enums.UserStatus;
import com.qros.modules.user.service.UserLookupService;
import com.qros.shared.cache.CacheNames;
import com.qros.shared.event.DomainEvents.UserAuthChangeEvent;
import com.qros.shared.security.CachedUserPrincipal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

class CustomUserDetailsServiceTest {

    @Test
    void loadsImmutablePasswordFreePrincipalSnapshot() {
        UserLookupService lookupService = mock(UserLookupService.class);
        LocalDateTime changedAt = LocalDateTime.of(2026, 11, 24, 10, 0);
        User user = User.builder()
                .id(42L)
                .email("chef@qros.vn")
                .password("$2a$10$secretHash")
                .role(UserRole.CHEF)
                .status(UserStatus.ACTIVE)
                .passwordChangedAt(changedAt)
                .mustChangePassword(true)
                .build();
        when(lookupService.findByEmailIgnoreCase("chef@qros.vn")).thenReturn(Optional.of(user));

        CustomUserDetailsService service = new CustomUserDetailsService(lookupService);
        UserDetails details = service.loadUserByUsername("  CHEF@QROS.VN ");

        assertThat(details).isInstanceOf(CachedUserPrincipal.class);
        CachedUserPrincipal principal = (CachedUserPrincipal) details;
        assertThat(principal.getUsername()).isEqualTo("chef@qros.vn");
        assertThat(principal.getPassword()).isEmpty();
        assertThat(principal.isEnabled()).isTrue();
        assertThat(principal.isAccountNonLocked()).isTrue();
        assertThat(principal.getPasswordChangedAt()).isEqualTo(changedAt);
        assertThat(principal.isMustChangePassword()).isTrue();
        assertThat(principal.getAuthorities()).extracting("authority").containsExactly("ROLE_CHEF");
    }

    @Test
    void rejectsBlankEmailWithoutQueryingRepository() {
        UserLookupService lookupService = mock(UserLookupService.class);
        CustomUserDetailsService service = new CustomUserDetailsService(lookupService);

        assertThatThrownBy(() -> service.loadUserByUsername("   ")).isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void userAuthChangeEventEvictsBothCurrentAndPreviousEmailKeys() {
        CacheManager cacheManager = mock(CacheManager.class);
        Cache authCache = mock(Cache.class);
        when(cacheManager.getCache(CacheNames.USER_AUTH)).thenReturn(authCache);

        new UserCacheInvalidationListener(cacheManager)
                .onUserAuthChange(new UserAuthChangeEvent("New@Qros.vn", "Old@Qros.vn"));

        verify(authCache).evictIfPresent("new@qros.vn");
        verify(authCache).evictIfPresent("old@qros.vn");
    }
}
