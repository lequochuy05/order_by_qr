package com.qros.modules.auth.service;

import com.qros.modules.auth.config.AuthLockoutProperties;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountLockoutService {

    private static final String LOCKOUT_ATTEMPTS_PREFIX = "auth:lockout:failed:";
    private static final String LOCKOUT_LOCKED_PREFIX = "auth:lockout:locked:";

    private static final DefaultRedisScript<Long> RECORD_FAILED_SCRIPT = new DefaultRedisScript<>(
            """
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            if current >= tonumber(ARGV[2]) then
                redis.call('SET', KEYS[2], 'locked', 'EX', ARGV[1])
                redis.call('DEL', KEYS[1])
            end
            return current
            """,
            Long.class);

    private final StringRedisTemplate redisTemplate;
    private final AuthLockoutProperties properties;

    /**
     * Checks if the account associated with the given email is currently locked.
     * Throws BusinessException with ACCOUNT_LOCKED if locked.
     *
     * @param email The account email
     */
    public void checkLocked(@NonNull String email) {
        String hash = digest(normalize(email));
        String lockedKey = LOCKOUT_LOCKED_PREFIX + hash;

        try {
            Long remainingSeconds = redisTemplate.getExpire(lockedKey, TimeUnit.SECONDS);
            if (remainingSeconds != null && remainingSeconds > 0) {
                long remainingMinutes = Math.max(1, (remainingSeconds + 59) / 60);
                throw new BusinessException(
                        ErrorCode.ACCOUNT_LOCKED,
                        "Account is temporarily locked due to exceeding " + properties.getMaxFailedAttempts()
                                + " failed attempts. Please try again after " + remainingMinutes + " minute(s).",
                        Map.of("retryAfterSeconds", remainingSeconds));
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            log.warn("Redis unavailable during lockout check for email hash={}: {}", hash, ex.getMessage());
        }
    }

    /**
     * Records a failed password attempt for the account.
     * If failed attempts reach the threshold, locks the account and throws ACCOUNT_LOCKED.
     *
     * @param email The account email
     */
    public void recordFailedAttempt(@NonNull String email) {
        String hash = digest(normalize(email));
        String attemptsKey = LOCKOUT_ATTEMPTS_PREFIX + hash;
        String lockedKey = LOCKOUT_LOCKED_PREFIX + hash;

        long durationSeconds = properties.getDuration().toSeconds();
        int maxAttempts = properties.getMaxFailedAttempts();

        try {
            Long currentAttempts = redisTemplate.execute(
                    RECORD_FAILED_SCRIPT,
                    List.of(attemptsKey, lockedKey),
                    Long.toString(durationSeconds),
                    Integer.toString(maxAttempts));

            if (currentAttempts != null && currentAttempts >= maxAttempts) {
                long remainingMinutes = Math.max(1, (durationSeconds + 59) / 60);
                throw new BusinessException(
                        ErrorCode.ACCOUNT_LOCKED,
                        "Account has been temporarily locked for " + remainingMinutes
                                + " minute(s) due to " + maxAttempts + " consecutive failed attempts.",
                        Map.of("retryAfterSeconds", durationSeconds));
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            log.warn("Redis unavailable during recording failed attempt for email hash={}: {}", hash, ex.getMessage());
        }
    }

    /**
     * Resets failed login attempts and removes any lockout upon a successful login.
     *
     * @param email The account email
     */
    public void recordSuccessfulLogin(@NonNull String email) {
        unlock(email);
    }

    /**
     * Unlocks the account and clears all failed attempt counters.
     *
     * @param email The account email
     */
    public void unlock(@NonNull String email) {
        String hash = digest(normalize(email));
        String attemptsKey = LOCKOUT_ATTEMPTS_PREFIX + hash;
        String lockedKey = LOCKOUT_LOCKED_PREFIX + hash;

        try {
            redisTemplate.delete(List.of(attemptsKey, lockedKey));
        } catch (RuntimeException ex) {
            log.warn("Redis unavailable during account unlock for email hash={}: {}", hash, ex.getMessage());
        }
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String digest(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", ex);
        }
    }
}
