package com.qros.modules.auth.service;

import com.qros.infrastructure.mail.EmailService;
import com.qros.modules.user.model.User;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.security.HashUtils;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class Admin2faService {

    private static final Logger log = LoggerFactory.getLogger(Admin2faService.class);

    private static final String TICKET_KEY_PREFIX = "qros:2fa:ticket:";
    private static final String COOLDOWN_KEY_PREFIX = "qros:2fa:cooldown:";
    private static final Duration TICKET_TTL = Duration.ofMinutes(5);
    private static final Duration COOLDOWN_TTL = Duration.ofSeconds(60);
    static final int MAX_ATTEMPTS = 5;
    static final int MAX_RESENDS = 3;

    private static final String FIELD_EMAIL = "email";
    private static final String FIELD_OTP_HASH = "otpHash";
    private static final String FIELD_ATTEMPTS = "attempts";
    private static final String FIELD_RESENDS = "resends";
    private static final String FIELD_EXPIRES_AT = "expiresAt";

    private static final DefaultRedisScript<String> VERIFY_OTP_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('EXISTS', KEYS[1]) == 0 then
                return 'EXPIRED'
            end
            local attempts = tonumber(redis.call('HGET', KEYS[1], 'attempts') or '0')
            local maxAttempts = tonumber(ARGV[2])
            if attempts >= maxAttempts then
                redis.call('DEL', KEYS[1], KEYS[2])
                return 'TOO_MANY'
            end
            local storedHash = redis.call('HGET', KEYS[1], 'otpHash')
            if storedHash ~= ARGV[1] then
                local nextAttempts = redis.call('HINCRBY', KEYS[1], 'attempts', 1)
                if nextAttempts >= maxAttempts then
                    redis.call('DEL', KEYS[1], KEYS[2])
                    return 'TOO_MANY'
                end
                return 'INVALID'
            end
            local email = redis.call('HGET', KEYS[1], 'email') or ''
            redis.call('DEL', KEYS[1], KEYS[2])
            return 'OK:' .. email
            """, String.class);

    private final StringRedisTemplate redisTemplate;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    public Admin2faService(StringRedisTemplate redisTemplate, EmailService emailService, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.emailService = emailService;
    }

    public String initiate2fa(@NonNull User user) {
        String preAuthToken = UUID.randomUUID().toString();
        String otp = generateOtp();
        long expiresAtEpochSec = Instant.now().plus(TICKET_TTL).getEpochSecond();

        saveTicketHash(preAuthToken, user.getEmail(), hashOtp(otp), 0, 0, expiresAtEpochSec, TICKET_TTL);
        setCooldown(preAuthToken);

        try {
            emailService.sendAdmin2faOtpEmail(user.getEmail(), otp);
            log.info("2FA code sent to admin account");
        } catch (Exception e) {
            redisTemplate.delete(ticketKey(preAuthToken));
            redisTemplate.delete(cooldownKey(preAuthToken));
            log.error("Failed to dispatch 2FA email: {}", e.getMessage(), e);
            throw new BusinessException(
                    ErrorCode.DEPENDENCY_UNAVAILABLE,
                    "Unable to send 2FA verification email. Please try again later.",
                    e);
        }

        return preAuthToken;
    }

    public String verify2fa(@NonNull String preAuthToken, @NonNull String inputOtp) {
        String ticketKey = ticketKey(preAuthToken);
        String cooldownKey = cooldownKey(preAuthToken);
        String inputHash = hashOtp(inputOtp.trim());

        String result = redisTemplate.execute(
                VERIFY_OTP_SCRIPT, List.of(ticketKey, cooldownKey), inputHash, String.valueOf(MAX_ATTEMPTS));

        if (result == null || "EXPIRED".equals(result)) {
            throw new BusinessException(ErrorCode.EXPIRED_2FA_TICKET);
        }
        if ("TOO_MANY".equals(result)) {
            throw new BusinessException(ErrorCode.TOO_MANY_2FA_ATTEMPTS);
        }
        if ("INVALID".equals(result)) {
            throw new BusinessException(ErrorCode.INVALID_2FA_CODE);
        }
        if (result.startsWith("OK:")) {
            return result.substring(3);
        }

        throw new BusinessException(ErrorCode.EXPIRED_2FA_TICKET);
    }

    public void resend2fa(@NonNull String preAuthToken) {
        String cooldown = cooldownKey(preAuthToken);
        Boolean acquiredCooldown = redisTemplate.opsForValue().setIfAbsent(cooldown, "1", COOLDOWN_TTL);
        if (!Boolean.TRUE.equals(acquiredCooldown)) {
            throw new BusinessException(ErrorCode.COOLDOWN_2FA);
        }

        String ticketKey = ticketKey(preAuthToken);
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(ticketKey);
        if (entries == null || entries.isEmpty()) {
            redisTemplate.delete(cooldown);
            throw new BusinessException(ErrorCode.EXPIRED_2FA_TICKET);
        }

        String email = stringField(entries, FIELD_EMAIL);
        int currentResends = intField(entries, FIELD_RESENDS);
        long expiresAtEpochSec = longField(entries, FIELD_EXPIRES_AT);

        if (email == null || email.isBlank()) {
            redisTemplate.delete(cooldown);
            throw new BusinessException(ErrorCode.EXPIRED_2FA_TICKET);
        }
        if (currentResends >= MAX_RESENDS) {
            redisTemplate.delete(cooldown);
            throw new BusinessException(
                    ErrorCode.TOO_MANY_2FA_ATTEMPTS, "Maximum 2FA code resend limit reached. Please log in again");
        }

        long remainingSeconds = expiresAtEpochSec - Instant.now().getEpochSecond();
        if (remainingSeconds <= 0) {
            redisTemplate.delete(ticketKey);
            redisTemplate.delete(cooldown);
            throw new BusinessException(ErrorCode.EXPIRED_2FA_TICKET);
        }

        String newOtp = generateOtp();
        try {
            emailService.sendAdmin2faOtpEmail(email, newOtp);
            log.info("2FA code re-sent to admin account");
        } catch (Exception e) {
            redisTemplate.delete(cooldown);
            log.error("Failed to re-dispatch 2FA email: {}", e.getMessage(), e);
            throw new BusinessException(
                    ErrorCode.DEPENDENCY_UNAVAILABLE,
                    "Unable to send 2FA verification email. Please try again later.",
                    e);
        }

        // Update only otpHash and atomically increment resends so concurrent verify2fa attempts are never overwritten.
        redisTemplate.opsForHash().put(ticketKey, FIELD_OTP_HASH, hashOtp(newOtp));
        redisTemplate.opsForHash().increment(ticketKey, FIELD_RESENDS, 1L);
        redisTemplate.expire(ticketKey, Duration.ofSeconds(Math.max(1L, remainingSeconds)));
    }

    public String getTicketEmail(@NonNull String preAuthToken) {
        Object email = redisTemplate.opsForHash().get(ticketKey(preAuthToken), FIELD_EMAIL);
        return email != null ? email.toString() : null;
    }

    private void saveTicketHash(
            String token,
            String email,
            String otpHash,
            int attempts,
            int resends,
            long expiresAtEpochSec,
            Duration ttl) {
        String key = ticketKey(token);
        redisTemplate
                .opsForHash()
                .putAll(
                        key,
                        Map.of(
                                FIELD_EMAIL, email,
                                FIELD_OTP_HASH, otpHash,
                                FIELD_ATTEMPTS, Integer.toString(attempts),
                                FIELD_RESENDS, Integer.toString(resends),
                                FIELD_EXPIRES_AT, Long.toString(expiresAtEpochSec)));
        redisTemplate.expire(key, ttl);
    }

    private String generateOtp() {
        int code = secureRandom.nextInt(1_000_000);
        return String.format("%06d", code);
    }

    static String hashOtp(String otp) {
        return HashUtils.sha256Hex(otp);
    }

    private void setCooldown(String token) {
        redisTemplate.opsForValue().set(cooldownKey(token), "1", COOLDOWN_TTL);
    }

    private String ticketKey(String token) {
        return TICKET_KEY_PREFIX + token;
    }

    private String cooldownKey(String token) {
        return COOLDOWN_KEY_PREFIX + token;
    }

    private String stringField(Map<Object, Object> entries, String key) {
        Object value = entries.get(key);
        return value != null ? value.toString() : null;
    }

    private int intField(Map<Object, Object> entries, String key) {
        String raw = stringField(entries, key);
        if (raw == null || raw.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private long longField(Map<Object, Object> entries, String key) {
        String raw = stringField(entries, key);
        if (raw == null || raw.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }
}
