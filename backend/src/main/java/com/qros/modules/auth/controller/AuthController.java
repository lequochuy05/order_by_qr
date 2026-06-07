package com.qros.modules.auth.controller;

import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.response.ApiResponse;
import com.qros.modules.auth.dto.AuthRequest;
import com.qros.modules.auth.dto.AuthResponse;
import com.qros.modules.user.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @Value("${security.jwt.refresh-cookie-name}")
    private String refreshCookieName;

    @Value("${security.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    @Value("${security.jwt.refresh-cookie-secure}")
    private boolean refreshCookieSecure;

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody AuthRequest req, HttpServletResponse response) {
        AuthResponse auth = userService.login(Objects.requireNonNull(req));
        setRefreshCookie(response, userService.issueRefreshToken(auth), Duration.ofMillis(refreshExpirationMs));
        return ApiResponse.success("Login successful", auth);
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractRefreshToken(request);
        AuthResponse auth = userService.refreshAccessToken(refreshToken);
        setRefreshCookie(response, userService.issueRefreshToken(auth), Duration.ofMillis(refreshExpirationMs));
        return ApiResponse.success("Token refreshed", auth);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        if (request.getCookies() != null) {
            Arrays.stream(request.getCookies())
                    .filter(cookie -> refreshCookieName.equals(cookie.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .ifPresent(userService::revokeRefreshToken);
        }
        setRefreshCookie(response, "", Duration.ZERO);
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
}
