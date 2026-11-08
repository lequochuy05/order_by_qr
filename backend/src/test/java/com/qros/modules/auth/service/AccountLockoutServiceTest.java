package com.qros.modules.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qros.modules.auth.config.AuthLockoutProperties;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

class AccountLockoutServiceTest {

    private StringRedisTemplate redisTemplate;
    private AuthLockoutProperties properties;
    private AccountLockoutService lockoutService;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        properties = new AuthLockoutProperties();
        properties.setMaxFailedAttempts(5);
        properties.setDuration(Duration.ofMinutes(15));
        lockoutService = new AccountLockoutService(redisTemplate, properties);
    }

    @Test
    void checkLockedThrowsAccountLockedWhenLockedKeyHasRemainingTtl() {
        when(redisTemplate.getExpire(anyString(), eq(TimeUnit.SECONDS))).thenReturn(600L);

        assertThatThrownBy(() -> lockoutService.checkLocked("user@example.com"))
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_LOCKED);
                    assertThat(ex.getDetails()).containsEntry("retryAfterSeconds", 600L);
                });
    }

    @Test
    void checkLockedDoesNothingWhenNotLocked() {
        when(redisTemplate.getExpire(anyString(), eq(TimeUnit.SECONDS))).thenReturn(-2L);

        assertThatCode(() -> lockoutService.checkLocked("user@example.com")).doesNotThrowAnyException();
    }

    @Test
    void checkLockedFailsOpenWhenRedisThrowsException() {
        when(redisTemplate.getExpire(anyString(), eq(TimeUnit.SECONDS)))
                .thenThrow(new RuntimeException("Redis connection refused"));

        assertThatCode(() -> lockoutService.checkLocked("user@example.com")).doesNotThrowAnyException();
    }

    @Test
    void recordFailedAttemptPassesWhenAttemptsUnderThreshold() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), anyString(), anyString()))
                .thenReturn(3L);

        assertThatCode(() -> lockoutService.recordFailedAttempt("user@example.com"))
                .doesNotThrowAnyException();
    }

    @Test
    void recordFailedAttemptThrowsAccountLockedWhenThresholdReached() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), anyString(), anyString()))
                .thenReturn(5L);

        assertThatThrownBy(() -> lockoutService.recordFailedAttempt("user@example.com"))
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_LOCKED);
                    assertThat(ex.getDetails()).containsEntry("retryAfterSeconds", 900L);
                });
    }

    @Test
    void recordFailedAttemptFailsOpenWhenRedisThrowsException() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Redis connection timeout"));

        assertThatCode(() -> lockoutService.recordFailedAttempt("user@example.com"))
                .doesNotThrowAnyException();
    }

    @Test
    void recordSuccessfulLoginDeletesAttemptsAndLockedKeys() {
        lockoutService.recordSuccessfulLogin("user@example.com");

        verify(redisTemplate).delete(anyList());
    }

    @Test
    void unlockDeletesAttemptsAndLockedKeys() {
        lockoutService.unlock("user@example.com");

        verify(redisTemplate).delete(anyList());
    }
}
