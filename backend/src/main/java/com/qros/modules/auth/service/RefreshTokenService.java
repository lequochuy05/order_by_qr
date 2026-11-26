package com.qros.modules.auth.service;

import com.qros.modules.auth.dto.internal.AuthenticatedUser;
import com.qros.modules.auth.dto.internal.RefreshResult;
import com.qros.modules.auth.dto.response.TokenResponse;
import com.qros.modules.auth.store.RefreshTokenStore;
import com.qros.modules.user.model.User;
import com.qros.modules.user.model.enums.UserStatus;
import com.qros.modules.user.service.UserLookupService;
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
    private final UserLookupService userRepository;
    private final JwtProperties jwtProperties;

    /**
     * Creates a new multi-device refresh token family for the authenticated user.
     * Each login gets its own family ID (fid) so multiple devices/KDS screens can
     * remain signed in concurrently while revoking all sessions on security events.
     */
    public String createRefreshToken(AuthenticatedUser authUser) {
        return createRefreshTokenForFamily(authUser, UUID.randomUUID().toString());
    }

    private String createRefreshTokenForFamily(AuthenticatedUser authUser, String familyId) {
        String userIndexKey = userSessionsIndexKey(authUser.userId());
        String sessionKey = sessionKey(authUser.userId(), familyId);
        String jti = UUID.randomUUID().toString();

        String refreshToken = jwtService.generateRefreshToken(
                authUser.email(),
                Map.of("uid", authUser.userId(), "role", authUser.role().name(), "jti", jti, "fid", familyId));

        refreshTokenStore.createSession(userIndexKey, sessionKey, jti, jwtProperties.getRefreshExpirationMs());

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
        String familyId = extractFamilyId(refreshToken);

        boolean consumed;
        if (familyId != null) {
            consumed = !jti.isBlank()
                    && refreshTokenStore.consumeSessionAtomically(
                            userSessionsIndexKey(userId), sessionKey(userId, familyId), jti);
        } else {
            // Backward compatibility for legacy single-slot tokens issued before multi-device support.
            consumed = !jti.isBlank() && refreshTokenStore.consumeAtomically(legacyKey(userId), jti);
        }

        if (!consumed) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN, "Refresh token has expired or was revoked");
        }

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
        String nextFamilyId = familyId != null ? familyId : UUID.randomUUID().toString();
        String newRefreshToken = createRefreshTokenForFamily(authUser, nextFamilyId);

        return new RefreshResult(TokenResponse.of(user, newAccessToken), newRefreshToken);
    }

    /**
     * Revokes only the device session associated with this refresh token.
     */
    public void revokeRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank() || !jwtService.isRefreshToken(refreshToken)) {
            return;
        }

        Object uid = jwtService.extractClaim(refreshToken, "uid");
        if (uid == null) {
            return;
        }
        Long userId = Long.valueOf(uid.toString());
        String familyId = extractFamilyId(refreshToken);
        if (familyId != null) {
            refreshTokenStore.revokeSession(userSessionsIndexKey(userId), sessionKey(userId, familyId));
        } else {
            refreshTokenStore.revoke(legacyKey(userId));
        }
    }

    /**
     * Revokes all active refresh tokens across every device for the specified user ID.
     * Used when an administrator deactivates, changes role, resets password, or deletes the user.
     */
    public void revokeUserTokens(Long userId) {
        if (userId != null) {
            refreshTokenStore.revokeAllUserSessions(userSessionsIndexKey(userId), legacyKey(userId));
        }
    }

    private String extractFamilyId(String refreshToken) {
        Object fid = jwtService.extractClaim(refreshToken, "fid");
        if (fid == null) {
            return null;
        }
        String value = fid.toString().trim();
        return value.isBlank() ? null : value;
    }

    private static String legacyKey(Long userId) {
        return "auth:refresh:user:" + userId;
    }

    private static String userSessionsIndexKey(Long userId) {
        return "auth:refresh:user:" + userId + ":sessions";
    }

    private static String sessionKey(Long userId, String familyId) {
        return "auth:refresh:user:" + userId + ":session:" + familyId;
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
        long issuedAtSec = issuedAt.getTime() / 1000;
        long passwordChangedAtSec = passwordChangedAt.atZone(AppTime.ZONE).toEpochSecond();
        return issuedAtSec < passwordChangedAtSec;
    }
}
