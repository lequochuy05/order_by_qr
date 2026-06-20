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
     *
     * @param authUser The authenticated user
     * @return The generated refresh token
     */
    public String createRefreshToken(AuthenticatedUser authUser) {
        String jti = UUID.randomUUID().toString();

        String refreshToken = jwtService.generateRefreshToken(
                authUser.email(),
                Map.of(
                        "uid", authUser.userId(),
                        "role", authUser.role().name(),
                        "jti", jti));

        refreshTokenStore.create(refreshTokenCacheKey(authUser.userId(), jti), jwtProperties.getRefreshExpirationMs());

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

        String cacheKey = refreshTokenCacheKey(userId, jti);

        if (jti.isBlank() || !refreshTokenStore.consumeAtomically(cacheKey)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN, "Refresh token has expired or was revoked");
        }

        User user = userRepository
                .findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

        if (user.getStatus() != UserStatus.ACTIVE || !user.isEnabled()) {
            throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE);
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
     * Revokes a refresh token, preventing it from being used for future access token
     * refreshes.
     * @param refreshToken The refresh token to revoke
     */
    public void revokeRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank() || !jwtService.isRefreshToken(refreshToken)) {
            return;
        }

        Object uid = jwtService.extractClaim(refreshToken, "uid");
        String jti = jwtService.extractJti(refreshToken);

        if (uid != null && jti != null) {
            refreshTokenStore.revoke(refreshTokenCacheKey(Long.valueOf(uid.toString()), jti));
        }
    }

    /**
     * Constructs the cache key for storing refresh token validity.
     *
     * @param userId The user ID associated with the refresh token
     * @param jti    The unique identifier (jti) of the refresh token
     * @return The constructed cache key
     */
    private String refreshTokenCacheKey(Long userId, String jti) {
        return "auth:refresh:" + userId + ":" + jti;
    }
}
