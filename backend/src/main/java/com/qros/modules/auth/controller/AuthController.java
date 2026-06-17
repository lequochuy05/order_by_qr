package com.qros.modules.auth.controller;

import com.qros.modules.auth.dto.internal.LoginResult;
import com.qros.modules.auth.dto.internal.RefreshResult;
import com.qros.modules.auth.dto.request.LoginRequest;
import com.qros.modules.auth.dto.response.LoginResponse;
import com.qros.modules.auth.dto.response.TokenResponse;
import com.qros.modules.auth.service.AuthService;
import com.qros.modules.auth.service.RefreshTokenService;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.response.ApiResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;

    @Value("${security.jwt.refresh-cookie-name}")
    private String refreshCookieName;

    @Value("${security.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    @Value("${security.jwt.refresh-cookie-secure}")
    private boolean refreshCookieSecure;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest req, HttpServletResponse response) {
        LoginResult result = authService.login(req);

        String refreshToken = refreshTokenService.createRefreshToken(result.authUser());

        setRefreshCookie(response, refreshToken, Duration.ofMillis(refreshExpirationMs));

        return ApiResponse.success("Login successful", result.response());
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(HttpServletRequest request, HttpServletResponse response) {
        String oldRefreshToken = extractRefreshToken(request);

        RefreshResult result = refreshTokenService.refreshAccessToken(oldRefreshToken);

        setRefreshCookie(response, result.refreshToken(), Duration.ofMillis(refreshExpirationMs));

        return ApiResponse.success("Token refreshed", result.response());
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        if (request.getCookies() != null) {
            Arrays.stream(request.getCookies())
                    .filter(cookie -> refreshCookieName.equals(cookie.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .ifPresent(refreshTokenService::revokeRefreshToken);
        }

        clearRefreshCookie(response);

        return ApiResponse.success("Logout successful", null);
    }

    private String extractRefreshToken(HttpServletRequest request) {
        if (request.getCookies() == null) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN, "Refresh token missing");
        }

        return Arrays.stream(request.getCookies())
                .filter(cookie -> refreshCookieName.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN, "Refresh token missing"));
    }

    private void setRefreshCookie(HttpServletResponse response, String token, Duration maxAge) {
        ResponseCookie cookie = ResponseCookie.from(refreshCookieName, token)
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite(refreshCookieSecure ? "None" : "Lax")
                .path("/api")
                .maxAge(maxAge)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        setRefreshCookie(response, "", Duration.ZERO);
    }
}
