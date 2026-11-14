package com.qros.modules.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qros.modules.auth.dto.internal.LoginResult;
import com.qros.modules.auth.dto.request.LoginRequest;
import com.qros.modules.user.model.User;
import com.qros.modules.user.model.enums.UserRole;
import com.qros.modules.user.model.enums.UserStatus;
import com.qros.modules.user.repository.UserRepository;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.security.JwtService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private AccountLockoutService accountLockoutService;
    private Admin2faService admin2faService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtService = mock(JwtService.class);
        accountLockoutService = mock(AccountLockoutService.class);
        admin2faService = mock(Admin2faService.class);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encodedDummyHashValueForTimingDefense123");

        authService =
                new AuthService(userRepository, passwordEncoder, jwtService, accountLockoutService, admin2faService);
    }

    @Test
    void loginWhenAccountIsLockedThrowsAccountLocked() {
        LoginRequest request = new LoginRequest("locked@example.com", "Secret123!");
        doThrow(new BusinessException(ErrorCode.ACCOUNT_LOCKED))
                .when(accountLockoutService)
                .checkLocked("locked@example.com");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOfSatisfying(BusinessException.class, ex -> assertThat(ex.getErrorCode())
                        .isEqualTo(ErrorCode.ACCOUNT_LOCKED));
    }

    @Test
    void loginWithNonExistentEmailComputesDummyHashToPreventTimingAttacks() {
        LoginRequest request = new LoginRequest("unknown@example.com", "Secret123!");
        when(userRepository.findByEmailIgnoreCase("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOfSatisfying(BusinessException.class, ex -> assertThat(ex.getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_CREDENTIALS));

        verify(passwordEncoder).matches(eq("Secret123!"), eq("$2a$10$encodedDummyHashValueForTimingDefense123"));
        verify(accountLockoutService).recordFailedAttempt("unknown@example.com");
    }

    @Test
    void loginWithWrongPasswordExecutesPasswordMatchesAndThrowsInvalidCredentials() {
        User user = createUser(
                1L,
                "user@example.com",
                "Test User",
                "$2a$10$realUserHashedPasswordInDatabase",
                UserRole.STAFF,
                UserStatus.ACTIVE,
                false);
        LoginRequest request = new LoginRequest("user@example.com", "WrongPassword123!");
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword123!", user.getPassword())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOfSatisfying(BusinessException.class, ex -> assertThat(ex.getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_CREDENTIALS));

        verify(passwordEncoder).matches("WrongPassword123!", user.getPassword());
        verify(accountLockoutService).recordFailedAttempt("user@example.com");
    }

    @Test
    void loginWithInactiveAccountThrowsAccountInactive() {
        User user = createUser(
                1L,
                "user@example.com",
                "Test User",
                "$2a$10$realUserHashedPasswordInDatabase",
                UserRole.STAFF,
                UserStatus.INACTIVE,
                false);
        LoginRequest request = new LoginRequest("user@example.com", "Secret123!");
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Secret123!", user.getPassword())).thenReturn(true);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOfSatisfying(BusinessException.class, ex -> assertThat(ex.getErrorCode())
                        .isEqualTo(ErrorCode.ACCOUNT_INACTIVE));
    }

    @Test
    void loginWithValidCredentialsReturnsLoginResult() {
        User user = createUser(
                1L,
                "user@example.com",
                "Test User",
                "$2a$10$realUserHashedPasswordInDatabase",
                UserRole.STAFF,
                UserStatus.ACTIVE,
                true);
        LoginRequest request = new LoginRequest("user@example.com", "Secret123!");
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Secret123!", user.getPassword())).thenReturn(true);
        when(jwtService.generateAccessToken(eq("user@example.com"), anyMap())).thenReturn("mock-access-token");

        LoginResult result = authService.login(request);

        assertThat(result).isNotNull();
        assertThat(result.response().accessToken()).isEqualTo("mock-access-token");
        assertThat(result.response().mustChangePassword()).isTrue();
        assertThat(result.authUser().userId()).isEqualTo(1L);
        verify(accountLockoutService).recordSuccessfulLogin("user@example.com");
    }

    @Test
    void loginWithAdminRoleInitiates2faAndReturnsPreAuthTokenWithoutSessionTokens() {
        User admin = createUser(
                99L,
                "owner@qros.vn",
                "Owner Admin",
                "$2a$10$realAdminHashedPassword",
                UserRole.ADMIN,
                UserStatus.ACTIVE,
                false);
        LoginRequest request = new LoginRequest("owner@qros.vn", "AdminSecret123!");
        when(userRepository.findByEmailIgnoreCase("owner@qros.vn")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("AdminSecret123!", admin.getPassword())).thenReturn(true);
        when(admin2faService.initiate2fa(admin)).thenReturn("pre-auth-uuid-12345");

        LoginResult result = authService.login(request);

        assertThat(result).isNotNull();
        assertThat(result.response().requires2fa()).isTrue();
        assertThat(result.response().preAuthToken()).isEqualTo("pre-auth-uuid-12345");
        assertThat(result.response().accessToken()).isNull();
        assertThat(result.authUser()).isNull();
        verify(accountLockoutService, org.mockito.Mockito.never()).recordSuccessfulLogin("owner@qros.vn");
        verify(admin2faService).initiate2fa(admin);
    }

    @Test
    void complete2faLoginWithActiveAdminIssuesAccessToken() {
        User admin = createUser(99L, "owner@qros.vn", "Owner Admin", null, UserRole.ADMIN, UserStatus.ACTIVE, false);
        when(userRepository.findByEmailIgnoreCase("owner@qros.vn")).thenReturn(Optional.of(admin));
        when(jwtService.generateAccessToken(eq("owner@qros.vn"), anyMap())).thenReturn("admin-access-token");

        LoginResult result = authService.complete2faLogin("owner@qros.vn");

        assertThat(result).isNotNull();
        assertThat(result.response().requires2fa()).isFalse();
        assertThat(result.response().accessToken()).isEqualTo("admin-access-token");
        assertThat(result.authUser()).isNotNull();
        assertThat(result.authUser().userId()).isEqualTo(99L);
        assertThat(result.authUser().role()).isEqualTo(UserRole.ADMIN);
        verify(accountLockoutService).recordSuccessfulLogin("owner@qros.vn");
    }

    private User createUser(
            Long id,
            String email,
            String fullName,
            String password,
            UserRole role,
            UserStatus status,
            boolean mustChangePassword) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setFullName(fullName);
        user.setPassword(password);
        user.setRole(role);
        user.setStatus(status);
        user.setMustChangePassword(mustChangePassword);
        return user;
    }
}
