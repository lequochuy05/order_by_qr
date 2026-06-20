package com.qros.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.qros.modules.auth.dto.internal.AuthenticatedUser;
import com.qros.modules.auth.dto.internal.RefreshResult;
import com.qros.modules.auth.service.RefreshTokenService;
import com.qros.modules.auth.store.RefreshTokenStore;
import com.qros.modules.user.model.User;
import com.qros.modules.user.model.enums.UserRole;
import com.qros.modules.user.model.enums.UserStatus;
import com.qros.modules.user.repository.UserRepository;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.security.JwtProperties;
import com.qros.shared.security.JwtService;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RefreshTokenServiceTest {

    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    private InMemoryRefreshTokenStore tokenStore;
    private RefreshTokenService refreshTokenService;
    private User user;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = jwtProperties();
        JwtService jwtService = new JwtService(jwtProperties);
        UserRepository userRepository = mock(UserRepository.class);

        user = User.builder()
                .id(7L)
                .email("manager@example.com")
                .fullName("Manager")
                .password("encoded")
                .role(UserRole.MANAGER)
                .status(UserStatus.ACTIVE)
                .build();

        when(userRepository.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));

        tokenStore = new InMemoryRefreshTokenStore();
        refreshTokenService = new RefreshTokenService(tokenStore, jwtService, userRepository, jwtProperties);
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    void concurrentRefreshAllowsExactlyOneRequestToRotateToken() throws Exception {
        String refreshToken = refreshTokenService.createRefreshToken(AuthenticatedUser.from(user));
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Future<Boolean> first = executor.submit(() -> attemptRefresh(refreshToken, ready, start));
        Future<Boolean> second = executor.submit(() -> attemptRefresh(refreshToken, ready, start));

        ready.await();
        start.countDown();

        assertThat(List.of(first.get(), second.get())).containsExactlyInAnyOrder(true, false);
    }

    @Test
    void rotatedTokenCannotBeReused() {
        String tokenA = refreshTokenService.createRefreshToken(AuthenticatedUser.from(user));

        RefreshResult firstRotation = refreshTokenService.refreshAccessToken(tokenA);

        assertThat(firstRotation.refreshToken()).isNotBlank().isNotEqualTo(tokenA);
        assertThatThrownBy(() -> refreshTokenService.refreshAccessToken(tokenA))
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN));
    }

    @Test
    void revokedTokenCannotBeRefreshed() {
        String refreshToken = refreshTokenService.createRefreshToken(AuthenticatedUser.from(user));

        refreshTokenService.revokeRefreshToken(refreshToken);

        assertThatThrownBy(() -> refreshTokenService.refreshAccessToken(refreshToken))
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN));
    }

    private boolean attemptRefresh(String refreshToken, CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        start.await();

        try {
            refreshTokenService.refreshAccessToken(refreshToken);
            return true;
        } catch (BusinessException exception) {
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
            return false;
        }
    }

    private JwtProperties jwtProperties() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("01234567890123456789012345678901");
        properties.setExpirationMs(60_000);
        properties.setRefreshExpirationMs(3_600_000);
        properties.setRefreshCookieName("refresh_token");
        return properties;
    }

    private static class InMemoryRefreshTokenStore implements RefreshTokenStore {

        private final Set<String> activeKeys = ConcurrentHashMap.newKeySet();

        @Override
        public void create(String key, long ttlMs) {
            activeKeys.add(key);
        }

        @Override
        public boolean consumeAtomically(String key) {
            return activeKeys.remove(key);
        }

        @Override
        public void revoke(String key) {
            activeKeys.remove(key);
        }
    }
}
