package com.qros.modules.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qros.core.config.security.CookieRequestOriginValidator;
import com.qros.modules.auth.dto.internal.AuthenticatedUser;
import com.qros.modules.auth.dto.internal.LoginResult;
import com.qros.modules.auth.dto.request.LoginRequest;
import com.qros.modules.auth.dto.request.Resend2faRequest;
import com.qros.modules.auth.dto.request.Verify2faRequest;
import com.qros.modules.auth.dto.response.LoginResponse;
import com.qros.modules.auth.service.Admin2faService;
import com.qros.modules.auth.service.AuthRateLimitService;
import com.qros.modules.auth.service.AuthService;
import com.qros.modules.auth.service.PasswordResetService;
import com.qros.modules.auth.service.RefreshTokenService;
import com.qros.modules.auth.store.RedisRefreshTokenStore;
import com.qros.modules.user.model.enums.UserRole;
import com.qros.modules.user.repository.UserRepository;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.rate_limit.ClientAddressResolver;
import com.qros.shared.response.ApiResponse;
import com.qros.shared.security.JwtProperties;
import com.qros.shared.security.JwtService;
import com.qros.shared.security.TokenBlacklistService;
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
                clientAddressResolver,
                mock(CookieRequestOriginValidator.class),
                mock(TokenBlacklistService.class));

        assertThatThrownBy(
                        () -> controller.login(request, mock(jakarta.servlet.http.HttpServletRequest.class), response))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Refresh token store is unavailable");
        verify(response, never()).addHeader(anyString(), anyString());
    }

    @Test
    void forgotPasswordDoesNotLeakEmailNotFound() {
        PasswordResetService passwordResetService = mock(PasswordResetService.class);
        doThrow(new BusinessException(ErrorCode.EMAIL_NOT_FOUND))
                .when(passwordResetService)
                .createPasswordResetToken("unknown@example.com");

        AuthRateLimitService authRateLimitService = mock(AuthRateLimitService.class);
        ClientAddressResolver clientAddressResolver = mock(ClientAddressResolver.class);
        when(clientAddressResolver.resolve(any())).thenReturn("127.0.0.1");

        AuthController controller = new AuthController(
                mock(AuthService.class),
                authRateLimitService,
                mock(RefreshTokenService.class),
                passwordResetService,
                jwtProperties(),
                clientAddressResolver,
                mock(CookieRequestOriginValidator.class),
                mock(TokenBlacklistService.class));

        ApiResponse<Void> response =
                controller.forgotPassword("unknown@example.com", mock(jakarta.servlet.http.HttpServletRequest.class));

        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage())
                .isEqualTo("If your email exists in our system, you will receive password reset instructions.");
    }

    @Test
    void logoutBlacklistsBearerAccessTokenAndRevokesRefreshToken() {
        TokenBlacklistService tokenBlacklistService = mock(TokenBlacklistService.class);
        RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);
        JwtProperties jwtProperties = jwtProperties();

        AuthController controller = new AuthController(
                mock(AuthService.class),
                mock(AuthRateLimitService.class),
                refreshTokenService,
                mock(PasswordResetService.class),
                jwtProperties,
                mock(ClientAddressResolver.class),
                mock(CookieRequestOriginValidator.class),
                tokenBlacklistService);

        jakarta.servlet.http.HttpServletRequest request = mock(jakarta.servlet.http.HttpServletRequest.class);
        jakarta.servlet.http.HttpServletResponse response = mock(jakarta.servlet.http.HttpServletResponse.class);

        when(request.getHeader("Authorization")).thenReturn("Bearer access-token-to-blacklist");
        jakarta.servlet.http.Cookie refreshCookie =
                new jakarta.servlet.http.Cookie("refresh_token", "refresh-token-val");
        when(request.getCookies()).thenReturn(new jakarta.servlet.http.Cookie[] {refreshCookie});

        controller.logout(request, response);

        verify(tokenBlacklistService).blacklistToken("access-token-to-blacklist");
        verify(refreshTokenService).revokeRefreshToken("refresh-token-val");
    }

    @Test
    void loginWith2faRequiredDoesNotSetRefreshCookieAndReturnsTicket() {
        AuthService authService = mock(AuthService.class);
        AuthRateLimitService authRateLimitService = mock(AuthRateLimitService.class);
        RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);
        ClientAddressResolver clientAddressResolver = mock(ClientAddressResolver.class);
        Admin2faService admin2faService = mock(Admin2faService.class);
        jakarta.servlet.http.HttpServletResponse response = mock(jakarta.servlet.http.HttpServletResponse.class);
        jakarta.servlet.http.HttpServletRequest request = mock(jakarta.servlet.http.HttpServletRequest.class);

        when(clientAddressResolver.resolve(any())).thenReturn("127.0.0.1");
        LoginRequest loginReq = new LoginRequest("admin@qros.vn", "password");
        LoginResponse loginResp = LoginResponse.require2fa("a***n@qros.vn", "ticket-uuid-123");
        when(authService.login(loginReq)).thenReturn(new LoginResult(loginResp, null));

        AuthController controller = new AuthController(
                authService,
                authRateLimitService,
                refreshTokenService,
                mock(PasswordResetService.class),
                jwtProperties(),
                clientAddressResolver,
                mock(CookieRequestOriginValidator.class),
                mock(TokenBlacklistService.class),
                admin2faService);

        ApiResponse<LoginResponse> result = controller.login(loginReq, request, response);

        assertThat(result).isNotNull();
        assertThat(result.getData().requires2fa()).isTrue();
        assertThat(result.getData().preAuthToken()).isEqualTo("ticket-uuid-123");
        verify(refreshTokenService, never()).createRefreshToken(any());
        verify(response, never()).addHeader(anyString(), anyString());
    }

    @Test
    void verify2faSuccessSetsRefreshCookieAndReturnsLoginResponse() {
        AuthService authService = mock(AuthService.class);
        AuthRateLimitService authRateLimitService = mock(AuthRateLimitService.class);
        RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);
        ClientAddressResolver clientAddressResolver = mock(ClientAddressResolver.class);
        Admin2faService admin2faService = mock(Admin2faService.class);
        jakarta.servlet.http.HttpServletResponse response = mock(jakarta.servlet.http.HttpServletResponse.class);
        jakarta.servlet.http.HttpServletRequest request = mock(jakarta.servlet.http.HttpServletRequest.class);

        when(clientAddressResolver.resolve(any())).thenReturn("127.0.0.1");
        when(admin2faService.getTicketEmail("ticket-uuid-123")).thenReturn("admin@qros.vn");
        when(admin2faService.verify2fa("ticket-uuid-123", "654321")).thenReturn("admin@qros.vn");

        AuthenticatedUser authUser = new AuthenticatedUser(10L, "admin@qros.vn", UserRole.ADMIN);
        LoginResponse finalLoginResp =
                new LoginResponse(10L, "Admin", UserRole.ADMIN, "admin-jwt-token", null, "admin@qros.vn");
        when(authService.complete2faLogin("admin@qros.vn")).thenReturn(new LoginResult(finalLoginResp, authUser));
        when(refreshTokenService.createRefreshToken(authUser)).thenReturn("admin-refresh-token");

        AuthController controller = new AuthController(
                authService,
                authRateLimitService,
                refreshTokenService,
                mock(PasswordResetService.class),
                jwtProperties(),
                clientAddressResolver,
                mock(CookieRequestOriginValidator.class),
                mock(TokenBlacklistService.class),
                admin2faService);

        Verify2faRequest verifyReq = new Verify2faRequest("ticket-uuid-123", "654321");
        ApiResponse<LoginResponse> result = controller.verify2fa(verifyReq, request, response);

        assertThat(result).isNotNull();
        assertThat(result.getData().accessToken()).isEqualTo("admin-jwt-token");
        assertThat(result.getData().requires2fa()).isFalse();
        verify(authRateLimitService).checkLogin("127.0.0.1", "admin@qros.vn");
        verify(refreshTokenService).createRefreshToken(authUser);
        verify(response).addHeader(eq("Set-Cookie"), anyString());
    }

    @Test
    void resend2faDelegatesToAdmin2faService() {
        Admin2faService admin2faService = mock(Admin2faService.class);
        AuthRateLimitService authRateLimitService = mock(AuthRateLimitService.class);
        ClientAddressResolver clientAddressResolver = mock(ClientAddressResolver.class);
        when(clientAddressResolver.resolve(any())).thenReturn("127.0.0.1");
        when(admin2faService.getTicketEmail("ticket-uuid-123")).thenReturn("admin@qros.vn");

        AuthController controller = new AuthController(
                mock(AuthService.class),
                authRateLimitService,
                mock(RefreshTokenService.class),
                mock(PasswordResetService.class),
                jwtProperties(),
                clientAddressResolver,
                mock(CookieRequestOriginValidator.class),
                mock(TokenBlacklistService.class),
                admin2faService);

        Resend2faRequest resendReq = new Resend2faRequest("ticket-uuid-123");
        ApiResponse<Void> result = controller.resend2fa(resendReq, mock(jakarta.servlet.http.HttpServletRequest.class));

        assertThat(result).isNotNull();
        verify(authRateLimitService).checkLogin("127.0.0.1", "admin@qros.vn");
        verify(admin2faService).resend2fa("ticket-uuid-123");
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
