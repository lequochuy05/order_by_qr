package com.qros.shared.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

class DistributedSchedulerLockTest {

    @Test
    @SuppressWarnings("unchecked")
    void acquiresExecutesAndReleasesLockWhenRedisGrantsLock() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(eq("qros:scheduler:lock:test-job"), anyString(), eq(Duration.ofMinutes(2))))
                .thenReturn(true);

        DistributedSchedulerLock lock = new DistributedSchedulerLock(redisTemplate);
        AtomicInteger runs = new AtomicInteger();

        boolean executed = lock.runWithLock("test-job", Duration.ofMinutes(2), runs::incrementAndGet);

        assertThat(executed).isTrue();
        assertThat(runs).hasValue(1);
        verify(redisTemplate).execute(any(RedisScript.class), eq(List.of("qros:scheduler:lock:test-job")), anyString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void skipsExecutionWhenAnotherInstanceHoldsLock() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(eq("qros:scheduler:lock:test-job"), anyString(), eq(Duration.ofMinutes(2))))
                .thenReturn(false);

        DistributedSchedulerLock lock = new DistributedSchedulerLock(redisTemplate);
        AtomicInteger runs = new AtomicInteger();

        boolean executed = lock.runWithLock("test-job", Duration.ofMinutes(2), runs::incrementAndGet);

        assertThat(executed).isFalse();
        assertThat(runs).hasValue(0);
        verify(redisTemplate, never()).execute(any(RedisScript.class), any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void failsOpenWhenRedisIsUnavailable() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenThrow(new RuntimeException("Redis down"));

        DistributedSchedulerLock lock = new DistributedSchedulerLock(redisTemplate);
        AtomicInteger runs = new AtomicInteger();

        boolean executed = lock.runWithLock("test-job", Duration.ofMinutes(2), runs::incrementAndGet);

        assertThat(executed).isTrue();
        assertThat(runs).hasValue(1);
    }
}
