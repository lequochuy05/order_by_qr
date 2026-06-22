package com.qros.modules.auth.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qros.modules.auth.dto.internal.AuthenticatedUser;
import com.qros.modules.auth.dto.internal.LoginResult;
import com.qros.modules.auth.dto.request.LoginRequest;
import com.qros.modules.auth.dto.response.LoginResponse;
import com.qros.modules.auth.service.AuthRateLimitService;
import com.qros.modules.auth.service.AuthService;
import com.qros.modules.auth.service.PasswordResetService;
import com.qros.modules.auth.service.RefreshTokenService;
import com.qros.modules.auth.store.RedisRefreshTokenStore;
import com.qros.modules.user.model.enums.UserRole;
import com.qros.modules.user.repository.UserRepository;
import com.qros.shared.rate_limit.ClientAddressResolver;
import com.qros.shared.security.JwtProperties;
import com.qros.shared.security.JwtService;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class AuthControllerTest {

    @Test
    @SuppressWarnings("unchecked")
    void redisFailureDuringLoginDoesNotIssueRefreshCookie() {
        JwtProperties jwtProperties = jwtProperties();
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doThrow(new RedisConnectionFailureException("Redis unavailable"))
                .when(valueOperations)
                .set(anyString(), anyString(), any(Duration.class));

        RefreshTokenService refreshTokenService = new RefreshTokenService(
                new RedisRefreshTokenStore(redisTemplate),
                new JwtService(jwtProperties),
                mock(UserRepository.class),
                jwtProperties);

        AuthService authService = mock(AuthService.class);
        AuthRateLimitService authRateLimitService = mock(AuthRateLimitService.class);
        PasswordResetService passwordResetService = mock(PasswordResetService.class);
        ClientAddressResolver clientAddressResolver = mock(ClientAddressResolver.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(clientAddressResolver.resolve(any())).thenReturn("127.0.0.1");
        LoginRequest request = new LoginRequest("manager@example.com", "password");
        AuthenticatedUser authUser = new AuthenticatedUser(7L, "manager@example.com", UserRole.MANAGER);
        LoginResponse loginResponse =
                new LoginResponse(7L, "Manager", UserRole.MANAGER, "access-token", null, "manager@example.com");
        when(authService.login(request)).thenReturn(new LoginResult(loginResponse, authUser));

        AuthController controller = new AuthController(
                authService,
                authRateLimitService,
                refreshTokenService,
                passwordResetService,
                jwtProperties,
                clientAddressResolver);

        assertThatThrownBy(
                        () -> controller.login(request, mock(jakarta.servlet.http.HttpServletRequest.class), response))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Refresh token store is unavailable");
        verify(response, never()).addHeader(anyString(), anyString());
    }

    private JwtProperties jwtProperties() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("01234567890123456789012345678901");
        properties.setExpirationMs(60_000);
        properties.setRefreshExpirationMs(3_600_000);
        properties.setRefreshCookieName("refresh_token");
        return properties;
    }
}
