package com.sacmauquan.qrordering.controller;

import com.sacmauquan.qrordering.dto.*;
import com.sacmauquan.qrordering.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * UserController - Handles user authentication, profile management, and staff
 * administration.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Value("${security.jwt.refresh-cookie-name}")
    private String refreshCookieName;

    @Value("${security.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    @Value("${security.jwt.refresh-cookie-secure}")
    private boolean refreshCookieSecure;

    /**
     * Authenticates a user and returns a JWT token along with basic user
     * information.
     * 
     * @param req Authentication request containing email and password
     * @return AuthResponse containing token and user details
     */
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

    /**
     * Retrieves a list of all staff members (Managers, Staff, Chefs).
     * 
     * @return List of UserResponse objects
     */
    @GetMapping
    public ApiResponse<List<UserResponse>> list() {
        return ApiResponse.success(userService.findAll());
    }

    /**
     * Retrieves detailed information of a specific user by ID.
     * 
     * @param id User ID
     * @return UserResponse object
     */
    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getOne(@PathVariable @NonNull Long id) {
        return ApiResponse.success(userService.getOne(id));
    }

    /**
     * Creates a new staff account.
     * 
     * @param req User data for creation
     * @return Created UserResponse object
     */
    @PostMapping
    public ApiResponse<UserResponse> create(@Valid @RequestBody UserUpsertRequest req) {
        return ApiResponse.success("Staff account created successfully",
                userService.create(Objects.requireNonNull(req)));
    }

    /**
     * Updates personal information or role of a staff member.
     * 
     * @param id  User ID to update
     * @param req Updated user data
     * @return Updated UserResponse object
     */
    @PatchMapping("/{id}")
    public ApiResponse<UserResponse> update(
            @PathVariable @NonNull Long id,
            @Valid @RequestBody UserUpsertRequest req) {
        return ApiResponse.success("Profile updated successfully",
                userService.update(id, Objects.requireNonNull(req)));
    }

    /**
     * Resets the password for a staff member.
     * Password should be provided in the request body for security.
     * 
     * @param id   User ID to reset password
     * @param body Request body containing the new password
     * @return Void success response
     */
    @PatchMapping("/{id}/reset-password")
    public ApiResponse<Void> resetPassword(
            @PathVariable @NonNull Long id,
            @Valid @RequestBody @NonNull AuthRequest body) {
        userService.resetPassword(id, Objects.requireNonNull(body.getPassword()));
        return ApiResponse.success("Password reset successfully", null);
    }

    /**
     * Deletes a staff account from the system using soft delete.
     * 
     * @param id User ID to delete
     * @return Void success response
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable @NonNull Long id) {
        userService.delete(id);
        return ApiResponse.success("Staff member removed from system", null);
    }

    /**
     * Updates the avatar image of a staff member.
     * 
     * @param id   User ID
     * @param file Image file to upload
     * @return Updated UserResponse object
     */
    @PostMapping("/{id}/avatar")
    public ApiResponse<UserResponse> uploadAvatar(
            @PathVariable @NonNull Long id,
            @RequestParam("file") @NonNull MultipartFile file) {
        UserResponse updated = userService.uploadAvatar(id, Objects.requireNonNull(file));
        return ApiResponse.success("Avatar updated successfully", updated);
    }

    private String extractRefreshToken(HttpServletRequest request) {
        if (request.getCookies() == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "Refresh token missing");
        }
        return Arrays.stream(request.getCookies())
                .filter(cookie -> refreshCookieName.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.UNAUTHORIZED, "Refresh token missing"));
    }

    private void setRefreshCookie(HttpServletResponse response, String token, Duration maxAge) {
        ResponseCookie cookie = ResponseCookie.from(refreshCookieName, token)
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite(refreshCookieSecure ? "None" : "Lax")
                .path("/api/users")
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
