package com.qros.shared.rate_limit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

class RedisRateLimiterTest {

    @Test
    void allowsRequestWhenRedisScriptReturnsNegativeOne() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.execute(any(), anyList(), eq("60000"), eq("5"))).thenReturn(-1L);

        RedisRateLimiter limiter = new RedisRateLimiter(redisTemplate);

        limiter.requireAllowed("auth:login", "127.0.0.1", 5, Duration.ofMinutes(1));
    }

    @Test
    void exposesRetryAfterWhenLimitIsExceeded() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.execute(any(), anyList(), eq("60000"), eq("5"))).thenReturn(1_200L);

        RedisRateLimiter limiter = new RedisRateLimiter(redisTemplate);

        assertThatThrownBy(() -> limiter.requireAllowed("auth:login", "127.0.0.1", 5, Duration.ofMinutes(1)))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RATE_LIMIT_EXCEEDED);
                    assertThat(exception.getDetails()).containsEntry("retryAfterSeconds", 2L);
                });
    }
}
