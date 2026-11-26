package com.qros.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qros.infrastructure.mail.EmailService;
import com.qros.modules.auth.dto.internal.AuthenticatedUser;
import com.qros.modules.auth.dto.internal.RefreshResult;
import com.qros.modules.auth.service.Admin2faService;
import com.qros.modules.auth.service.RefreshTokenService;
import com.qros.modules.auth.store.RedisRefreshTokenStore;
import com.qros.modules.user.model.User;
import com.qros.modules.user.model.enums.UserRole;
import com.qros.modules.user.model.enums.UserStatus;
import com.qros.modules.user.service.UserLookupService;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.ratelimit.RedisRateLimiter;
import com.qros.shared.security.JwtProperties;
import com.qros.shared.security.JwtService;
import com.redis.testcontainers.RedisContainer;
import java.time.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.json.JsonMapper;

@Testcontainers(disabledWithoutDocker = true)
class RedisAuthAndConcurrencyIT {

    @Container
    static final RedisContainer REDIS = new RedisContainer("redis:7-alpine");

    private static LettuceConnectionFactory connectionFactory;
    private static StringRedisTemplate redisTemplate;

    @BeforeAll
    static void setUpRedis() {
        RedisStandaloneConfiguration config =
                new RedisStandaloneConfiguration(REDIS.getRedisHost(), REDIS.getRedisPort());
        connectionFactory = new LettuceConnectionFactory(config);
        connectionFactory.afterPropertiesSet();
        redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();
    }

    @AfterAll
    static void tearDownRedis() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @Test
    void multiDeviceRefreshTokensRotateIndependentlyAndRevokeAllClearsEveryDevice() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret("01234567890123456789012345678901");
        jwtProperties.setExpirationMs(60_000);
        jwtProperties.setRefreshExpirationMs(3_600_000);

        JwtService jwtService = new JwtService(jwtProperties);
        UserLookupService userLookupService = mock(UserLookupService.class);
        User manager = User.builder()
                .id(77L)
                .email("manager-it@qros.vn")
                .fullName("Manager Multi-Device")
                .role(UserRole.MANAGER)
                .status(UserStatus.ACTIVE)
                .build();
        when(userLookupService.findByEmailIgnoreCase("manager-it@qros.vn")).thenReturn(java.util.Optional.of(manager));

        RefreshTokenService refreshTokenService = new RefreshTokenService(
                new RedisRefreshTokenStore(redisTemplate), jwtService, userLookupService, jwtProperties);
        AuthenticatedUser principal = new AuthenticatedUser(77L, "manager-it@qros.vn", UserRole.MANAGER);

        // Two devices log in concurrently with the same manager account
        String deviceAToken1 = refreshTokenService.createRefreshToken(principal);
        String deviceBToken1 = refreshTokenService.createRefreshToken(principal);

        // Rotating Device A succeeds and invalidates only Device A's old token
        RefreshResult deviceARotated = refreshTokenService.refreshAccessToken(deviceAToken1);
        String deviceAToken2 = deviceARotated.refreshToken();
        assertThat(deviceAToken2).isNotBlank().isNotEqualTo(deviceAToken1);

        // Device B is completely unaffected by Device A's rotation
        RefreshResult deviceBRotated = refreshTokenService.refreshAccessToken(deviceBToken1);
        String deviceBToken2 = deviceBRotated.refreshToken();
        assertThat(deviceBToken2).isNotBlank().isNotEqualTo(deviceBToken1);

        // Revoking Device A does NOT kick out Device B
        refreshTokenService.revokeRefreshToken(deviceAToken2);
        assertThatThrownBy(() -> refreshTokenService.refreshAccessToken(deviceAToken2))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN));

        RefreshResult deviceBRotatedAgain = refreshTokenService.refreshAccessToken(deviceBToken2);
        String deviceBToken3 = deviceBRotatedAgain.refreshToken();
        assertThat(deviceBToken3).isNotBlank();

        // Full account revocation (password change / ban) invalidates ALL remaining devices
        refreshTokenService.revokeUserTokens(77L);
        assertThatThrownBy(() -> refreshTokenService.refreshAccessToken(deviceBToken3))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN));
    }

    @Test
    void admin2faAtomicLuaVerificationAndResendCooldownPreserveAttemptsAgainstRealRedis() {
        EmailService emailService = mock(EmailService.class);
        Admin2faService admin2faService = new Admin2faService(redisTemplate, emailService, new JsonMapper());

        User admin = User.builder()
                .id(101L)
                .email("admin-it@qros.vn")
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .build();

        String ticket = admin2faService.initiate2fa(admin);

        // Immediate resend is atomically blocked by cooldown lock
        assertThatThrownBy(() -> admin2faService.resend2fa(ticket))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.COOLDOWN_2FA));

        // Wrong OTP executes real Redis Lua script and increments attempts -> 1
        assertThatThrownBy(() -> admin2faService.verify2fa(ticket, "000000"))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_2FA_CODE));
        assertThat(redisTemplate.opsForHash().get("qros:2fa:ticket:" + ticket, "attempts"))
                .isEqualTo("1");

        // Expire cooldown, then resend -> preserves attempts = 1 while updating otpHash and resends = 1
        redisTemplate.delete("qros:2fa:cooldown:" + ticket);
        admin2faService.resend2fa(ticket);

        assertThat(redisTemplate.opsForHash().get("qros:2fa:ticket:" + ticket, "attempts"))
                .isEqualTo("1");
        assertThat(redisTemplate.opsForHash().get("qros:2fa:ticket:" + ticket, "resends"))
                .isEqualTo("1");

        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService, org.mockito.Mockito.times(2))
                .sendAdmin2faOtpEmail(eq("admin-it@qros.vn"), otpCaptor.capture());
        String latestOtp = otpCaptor.getAllValues().getLast();

        // Verifying with latest OTP succeeds and consumes ticket atomically
        assertThat(admin2faService.verify2fa(ticket, latestOtp)).isEqualTo("admin-it@qros.vn");
        assertThat(redisTemplate.hasKey("qros:2fa:ticket:" + ticket)).isFalse();
    }

    @Test
    void redisRateLimiterEnforcesFixedWindowAgainstRealRedis() {
        RedisRateLimiter rateLimiter = new RedisRateLimiter(redisTemplate);
        String subject = "it-client-" + System.nanoTime();

        rateLimiter.requireAllowed("it:test", subject, 3, Duration.ofSeconds(10), false);
        rateLimiter.requireAllowed("it:test", subject, 3, Duration.ofSeconds(10), false);
        rateLimiter.requireAllowed("it:test", subject, 3, Duration.ofSeconds(10), false);

        assertThatThrownBy(() -> rateLimiter.requireAllowed("it:test", subject, 3, Duration.ofSeconds(10), false))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.RATE_LIMIT_EXCEEDED));
    }
}
