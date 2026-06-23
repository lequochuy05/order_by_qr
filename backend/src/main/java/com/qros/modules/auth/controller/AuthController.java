package com.qros.modules.auth.controller;

import com.qros.modules.auth.dto.internal.LoginResult;
import com.qros.modules.auth.dto.internal.RefreshResult;
import com.qros.modules.auth.dto.request.EmailPasswordResetRequest;
import com.qros.modules.auth.dto.request.LoginRequest;
import com.qros.modules.auth.dto.request.PhonePasswordResetRequest;
import com.qros.modules.auth.dto.response.LoginResponse;
import com.qros.modules.auth.dto.response.TokenResponse;
import com.qros.modules.auth.service.AuthRateLimitService;
import com.qros.modules.auth.service.AuthService;
import com.qros.modules.auth.service.PasswordResetService;
import com.qros.modules.auth.service.RefreshTokenService;
import com.qros.shared.constants.ApiRoutes;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.rate_limit.ClientAddressResolver;
import com.qros.shared.response.ApiResponse;
import com.qros.shared.security.JwtProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiRoutes.AUTH)
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthRateLimitService authRateLimitService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordResetService passwordResetService;
    private final JwtProperties jwtProperties;
    private final ClientAddressResolver clientAddressResolver;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(
            @Valid @RequestBody LoginRequest req, HttpServletRequest request, HttpServletResponse response) {
        String clientAddress = clientAddressResolver.resolve(request);
        authRateLimitService.checkLogin(clientAddress, req.email());
        LoginResult result = authService.login(req);

        String refreshToken = refreshTokenService.createRefreshToken(result.authUser());

        setRefreshCookie(response, refreshToken, Duration.ofMillis(jwtProperties.getRefreshExpirationMs()));

        return ApiResponse.success("Login successful", result.response());
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(HttpServletRequest request, HttpServletResponse response) {
        String clientAddress = clientAddressResolver.resolve(request);
        authRateLimitService.checkRefresh(clientAddress);
        String oldRefreshToken = extractRefreshToken(request);

        RefreshResult result = refreshTokenService.refreshAccessToken(oldRefreshToken);

        setRefreshCookie(response, result.refreshToken(), Duration.ofMillis(jwtProperties.getRefreshExpirationMs()));

        return ApiResponse.success("Token refreshed", result.response());
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        if (request.getCookies() != null) {
            Arrays.stream(request.getCookies())
                    .filter(cookie -> jwtProperties.getRefreshCookieName().equals(cookie.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .ifPresent(refreshTokenService::revokeRefreshToken);
        }

        clearRefreshCookie(response);

        return ApiResponse.success("Logout successful", null);
    }

    @PostMapping("/forgot-password-email")
    public ApiResponse<Void> forgotPassword(@RequestParam @NonNull String email, HttpServletRequest request) {
        String clientAddress = clientAddressResolver.resolve(request);
        authRateLimitService.checkForgotPassword(clientAddress, email);
        try {
            passwordResetService.createPasswordResetToken(email);
        } catch (BusinessException e) {
            if (e.getErrorCode() == ErrorCode.EMAIL_NOT_FOUND) throw e;
        }
        return ApiResponse.success(
                "If your email exists in our system, you will receive password reset instructions.", null);
    }

    @PostMapping("/reset-password-email")
    public ApiResponse<Void> resetPassword(
            @Valid @RequestBody @NonNull EmailPasswordResetRequest req, HttpServletRequest request) {
        String clientAddress = clientAddressResolver.resolve(request);
        authRateLimitService.checkPasswordReset(clientAddress, req.token());
        passwordResetService.resetPassword(req.token(), req.newPassword());
        return ApiResponse.success("Password reset successfully.", null);
    }

    @PostMapping("/forgot-password-phone")
    public ApiResponse<Void> forgotPasswordPhone(@RequestParam @NonNull String phone, HttpServletRequest request) {
        String clientAddress = clientAddressResolver.resolve(request);
        authRateLimitService.checkForgotPassword(clientAddress, phone);
        try {
            passwordResetService.createOtpAndSendOtp(phone);
        } catch (BusinessException e) {
            if (e.getErrorCode() == ErrorCode.FEATURE_DISABLED) throw e;
        }
        return ApiResponse.success("If the phone number exists, an OTP code has been sent.", null);
    }

    @PostMapping("/reset-password-phone")
    public ApiResponse<Void> resetPasswordPhone(
            @Valid @RequestBody @NonNull PhonePasswordResetRequest req, HttpServletRequest request) {
        String clientAddress = clientAddressResolver.resolve(request);
        authRateLimitService.checkPasswordReset(clientAddress, req.phone());
        passwordResetService.resetPasswordWithOtp(req.phone(), req.otp(), req.newPassword());
        return ApiResponse.success("Password reset successfully.", null);
    }

    private String extractRefreshToken(HttpServletRequest request) {
        if (request.getCookies() == null) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN, "Refresh token missing");
        }

        return Arrays.stream(request.getCookies())
                .filter(cookie -> jwtProperties.getRefreshCookieName().equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN, "Refresh token missing"));
    }

    private void setRefreshCookie(HttpServletResponse response, String token, Duration maxAge) {
        ResponseCookie cookie = ResponseCookie.from(jwtProperties.getRefreshCookieName(), token)
                .httpOnly(true)
                .secure(jwtProperties.isRefreshCookieSecure())
                .sameSite(jwtProperties.getRefreshCookieSameSite())
                .path(ApiRoutes.PREFIX)
                .maxAge(maxAge)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        setRefreshCookie(response, "", Duration.ZERO);
    }
}
