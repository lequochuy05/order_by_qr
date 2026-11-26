package com.qros.shared.scheduling;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

/**
 * Lightweight Redis-backed distributed lock for {@code @Scheduled} jobs.
 * Prevents duplicate concurrent job runs when horizontally scaled across multiple replicas,
 * while failing open safely when Redis is unavailable (e.g., local dev or unit tests).
 */
@Slf4j
@Service
public class DistributedSchedulerLock {

    private static final String LOCK_KEY_PREFIX = "qros:scheduler:lock:";
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = createUnlockScript();

    private final StringRedisTemplate redisTemplate;

    @Autowired
    public DistributedSchedulerLock(ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        this.redisTemplate = redisTemplateProvider != null ? redisTemplateProvider.getIfAvailable() : null;
    }

    DistributedSchedulerLock(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public static DistributedSchedulerLock noOp() {
        return new DistributedSchedulerLock((StringRedisTemplate) null);
    }

    /**
     * Executes {@code task} only if this instance acquires the distributed lock for {@code lockName}.
     *
     * @return {@code true} if the task executed, {@code false} if another instance holds the lock
     */
    public boolean runWithLock(@NonNull String lockName, @NonNull Duration ttl, @NonNull Runnable task) {
        Objects.requireNonNull(lockName, "lockName must not be null");
        Objects.requireNonNull(ttl, "ttl must not be null");
        Objects.requireNonNull(task, "task must not be null");

        if (redisTemplate == null) {
            task.run();
            return true;
        }

        String lockKey = LOCK_KEY_PREFIX + lockName;
        String lockOwner = UUID.randomUUID().toString();
        boolean acquired;
        try {
            acquired = Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(lockKey, lockOwner, ttl));
        } catch (RuntimeException ex) {
            log.warn(
                    "Redis unavailable while acquiring scheduler lock '{}': {}. Running locally.",
                    lockName,
                    ex.getMessage());
            task.run();
            return true;
        }

        if (!acquired) {
            log.debug("Skipping scheduled job '{}' because another instance holds lock {}", lockName, lockKey);
            return false;
        }

        try {
            task.run();
            return true;
        } finally {
            releaseSilently(lockKey, lockOwner, lockName);
        }
    }

    private void releaseSilently(String lockKey, String lockOwner, String lockName) {
        try {
            redisTemplate.execute(UNLOCK_SCRIPT, List.of(lockKey), lockOwner);
        } catch (RuntimeException ex) {
            log.warn("Failed to release scheduler lock '{}': {}", lockName, ex.getMessage());
        }
    }

    private static DefaultRedisScript<Long> createUnlockScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(
                "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end");
        script.setResultType(Long.class);
        return script;
    }
}
