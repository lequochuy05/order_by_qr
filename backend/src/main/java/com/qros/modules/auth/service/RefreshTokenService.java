package com.qros.modules.auth.service;

import com.qros.modules.auth.dto.internal.AuthenticatedUser;
import com.qros.modules.auth.dto.internal.RefreshResult;
import com.qros.modules.auth.dto.response.TokenResponse;
import com.qros.modules.auth.store.RefreshTokenStore;
import com.qros.modules.user.model.User;
import com.qros.modules.user.model.enums.UserStatus;
import com.qros.modules.user.repository.UserRepository;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.security.JwtProperties;
import com.qros.shared.security.JwtService;
import com.qros.shared.time.AppTime;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenStore refreshTokenStore;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final JwtProperties jwtProperties;

    /**
     * Creates a new refresh token for the authenticated user.
     * Overwrites any existing refresh token for this user — the old token
     * is immediately invalidated.
     *
     * @param authUser The authenticated user
     * @return The generated refresh token
     */
    public String createRefreshToken(AuthenticatedUser authUser) {
        String key = "auth:refresh:user:" + authUser.userId();
        String jti = UUID.randomUUID().toString();

        String refreshToken = jwtService.generateRefreshToken(
                authUser.email(),
                Map.of(
                        "uid", authUser.userId(),
                        "role", authUser.role().name(),
                        "jti", jti));

        // SET ghi đè — lưu jti để verify khi refresh
        refreshTokenStore.create(key, jti, jwtProperties.getRefreshExpirationMs());

        return refreshToken;
    }

    /**
     * Refreshes the access token using a valid refresh token.
     *
     * @param refreshToken The refresh token
     * @return The result containing the new access token and refresh token
     */
    public RefreshResult refreshAccessToken(String refreshToken) {
        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        String email = jwtService.extractSubject(refreshToken);
        Long userId = Long.valueOf(jwtService.extractClaim(refreshToken, "uid").toString());
        String jti = Objects.toString(jwtService.extractJti(refreshToken), "");

        String key = "auth:refresh:user:" + userId;

        // So sánh jti trong token vs jti trong Redis
        String storedJti = refreshTokenStore.get(key);
        if (jti.isBlank() || storedJti == null || !storedJti.equals(jti)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN, "Refresh token has expired or was revoked");
        }

        // Xoá key cũ — không cho dùng lại token này
        refreshTokenStore.revoke(key);

        User user = userRepository
                .findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

        if (user.getStatus() != UserStatus.ACTIVE || !user.isEnabled()) {
            throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE);
        }
        if (wasIssuedBeforePasswordChange(refreshToken, user)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN, "Refresh token has expired or was revoked");
        }

        String newAccessToken = jwtService.generateAccessToken(
                user.getEmail(),
                Map.of(
                        "uid", user.getId(),
                        "role", user.getRole().name()));

        AuthenticatedUser authUser = AuthenticatedUser.from(user);
        String newRefreshToken = createRefreshToken(authUser);

        return new RefreshResult(TokenResponse.of(user, newAccessToken), newRefreshToken);
    }

    /**
     * Revokes the refresh token, preventing it from being used for future access token
     * refreshes.
     * @param refreshToken The refresh token to revoke
     */
    public void revokeRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank() || !jwtService.isRefreshToken(refreshToken)) {
            return;
        }

        Object uid = jwtService.extractClaim(refreshToken, "uid");

        if (uid != null) {
            refreshTokenStore.revoke("auth:refresh:user:" + uid);
        }
    }

    private boolean wasIssuedBeforePasswordChange(String refreshToken, User user) {
        LocalDateTime passwordChangedAt = user.getPasswordChangedAt();
        if (passwordChangedAt == null) {
            return false;
        }
        Date issuedAt = jwtService.extractIssuedAt(refreshToken);
        if (issuedAt == null) {
            return true;
        }
        LocalDateTime issuedAtLocal = LocalDateTime.ofInstant(issuedAt.toInstant(), AppTime.ZONE);
        return issuedAtLocal.isBefore(passwordChangedAt);
    }
}
