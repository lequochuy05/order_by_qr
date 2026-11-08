package com.qros.shared.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class TokenBlacklistServiceTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private JwtService jwtService;
    private TokenBlacklistService blacklistService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        jwtService = mock(JwtService.class);
        blacklistService = new TokenBlacklistService(redisTemplate, jwtService);
    }

    @Test
    void blacklistTokenStoresInRedisWithRemainingDuration() {
        String token = "sample-valid-token";
        when(jwtService.isValid(token)).thenReturn(true);
        Date futureExpiration = new Date(System.currentTimeMillis() + 60_000);
        when(jwtService.extractExpiration(token)).thenReturn(futureExpiration);

        blacklistService.blacklistToken(token);

        verify(valueOperations).set(startsWith("auth:blacklist:token:"), eq("revoked"), any(Duration.class));
    }

    @Test
    void blacklistTokenDoesNothingIfInvalid() {
        String token = "invalid-token";
        when(jwtService.isValid(token)).thenReturn(false);

        blacklistService.blacklistToken(token);

        verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void blacklistTokenDoesNothingIfAlreadyExpired() {
        String token = "expired-token";
        when(jwtService.isValid(token)).thenReturn(true);
        Date pastExpiration = new Date(System.currentTimeMillis() - 10_000);
        when(jwtService.extractExpiration(token)).thenReturn(pastExpiration);

        blacklistService.blacklistToken(token);

        verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void isBlacklistedReturnsTrueWhenKeyExistsInRedis() {
        String token = "blacklisted-token";
        when(redisTemplate.hasKey(startsWith("auth:blacklist:token:"))).thenReturn(true);

        assertThat(blacklistService.isBlacklisted(token)).isTrue();
    }

    @Test
    void isBlacklistedReturnsFalseWhenKeyDoesNotExistInRedis() {
        String token = "clean-token";
        when(redisTemplate.hasKey(startsWith("auth:blacklist:token:"))).thenReturn(false);

        assertThat(blacklistService.isBlacklisted(token)).isFalse();
    }

    @Test
    void isBlacklistedFailsOpenWhenRedisThrowsException() {
        String token = "token-during-redis-outage";
        when(redisTemplate.hasKey(startsWith("auth:blacklist:token:")))
                .thenThrow(new RedisConnectionFailureException("Redis down"));

        assertThat(blacklistService.isBlacklisted(token)).isFalse();
    }
}
