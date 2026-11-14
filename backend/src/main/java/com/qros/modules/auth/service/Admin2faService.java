package com.qros.modules.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qros.infrastructure.mail.SmtpEmailService;
import com.qros.modules.user.model.User;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Service
public class Admin2faService {

    private static final Logger log = LoggerFactory.getLogger(Admin2faService.class);

    private static final String TICKET_KEY_PREFIX = "qros:2fa:ticket:";
    private static final String COOLDOWN_KEY_PREFIX = "qros:2fa:cooldown:";
    private static final Duration TICKET_TTL = Duration.ofMinutes(5);
    private static final Duration COOLDOWN_TTL = Duration.ofSeconds(60);
    private static final int MAX_ATTEMPTS = 5;

    private final StringRedisTemplate redisTemplate;
    private final SmtpEmailService smtpEmailService;
    private final ObjectMapper objectMapper;
    private final Environment environment;
    private final SecureRandom secureRandom = new SecureRandom();

    public Admin2faService(
            StringRedisTemplate redisTemplate, SmtpEmailService smtpEmailService, ObjectMapper objectMapper) {
        this(redisTemplate, smtpEmailService, objectMapper, null);
    }

    @Autowired
    public Admin2faService(
            StringRedisTemplate redisTemplate,
            SmtpEmailService smtpEmailService,
            ObjectMapper objectMapper,
            Environment environment) {
        this.redisTemplate = redisTemplate;
        this.smtpEmailService = smtpEmailService;
        this.objectMapper = objectMapper;
        this.environment = environment;
    }

    public record TicketData(String email, String otp, int attempts) {}

    /**
     * Khởi tạo phiên 2FA cho tài khoản Quản trị:
     * - Tạo mã OTP 6 chữ số ngẫu nhiên
     * - Lưu ticket vào Redis (hết hạn trong 5 phút)
     * - Đặt cooldown 60 giây gửi lại
     * - Gửi email chứa mã OTP và log mã ở môi trường dev/local
     */
    public String initiate2fa(@NonNull User user) {
        String preAuthToken = UUID.randomUUID().toString();
        String otp = generateOtp();

        TicketData ticket = new TicketData(user.getEmail(), otp, 0);
        saveTicket(preAuthToken, ticket, TICKET_TTL);
        setCooldown(preAuthToken);

        sendOtpNotification(user.getEmail(), otp);

        return preAuthToken;
    }

    /**
     * Xác thực mã OTP 2FA:
     * - Kiểm tra vé preAuthToken trong Redis
     * - Giới hạn tối đa 5 lần thử sai để chống brute-force
     * - So sánh OTP theo cơ chế constant-time comparison
     * - Trả về email của quản trị viên nếu xác thực thành công
     */
    public String verify2fa(@NonNull String preAuthToken, @NonNull String inputOtp) {
        String ticketKey = ticketKey(preAuthToken);
        String rawTicket = redisTemplate.opsForValue().get(ticketKey);

        if (rawTicket == null) {
            throw new BusinessException(ErrorCode.EXPIRED_2FA_TICKET);
        }

        TicketData ticket = deserializeTicket(rawTicket);

        if (ticket.attempts() >= MAX_ATTEMPTS) {
            redisTemplate.delete(ticketKey);
            redisTemplate.delete(cooldownKey(preAuthToken));
            throw new BusinessException(ErrorCode.TOO_MANY_2FA_ATTEMPTS);
        }

        boolean otpMatches = MessageDigest.isEqual(
                ticket.otp().getBytes(StandardCharsets.UTF_8), inputOtp.trim().getBytes(StandardCharsets.UTF_8));

        if (!otpMatches) {
            int nextAttempts = ticket.attempts() + 1;
            Long remainingTtlSeconds = redisTemplate.getExpire(ticketKey, TimeUnit.SECONDS);
            Duration remainingDuration = (remainingTtlSeconds != null && remainingTtlSeconds > 0)
                    ? Duration.ofSeconds(remainingTtlSeconds)
                    : Duration.ofMinutes(1);

            if (nextAttempts >= MAX_ATTEMPTS) {
                redisTemplate.delete(ticketKey);
                redisTemplate.delete(cooldownKey(preAuthToken));
                throw new BusinessException(ErrorCode.TOO_MANY_2FA_ATTEMPTS);
            }

            saveTicket(preAuthToken, new TicketData(ticket.email(), ticket.otp(), nextAttempts), remainingDuration);
            throw new BusinessException(ErrorCode.INVALID_2FA_CODE);
        }

        // OTP đúng -> Dọn dẹp vé trong Redis
        redisTemplate.delete(ticketKey);
        redisTemplate.delete(cooldownKey(preAuthToken));

        return ticket.email();
    }

    /**
     * Gửi lại mã OTP 2FA mới:
     * - Áp dụng cooldown 60 giây để chống spam mail
     * - Sinh mã mới và làm mới hạn 5 phút cho ticket
     */
    public void resend2fa(@NonNull String preAuthToken) {
        String cooldown = cooldownKey(preAuthToken);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cooldown))) {
            throw new BusinessException(ErrorCode.COOLDOWN_2FA);
        }

        String ticketKey = ticketKey(preAuthToken);
        String rawTicket = redisTemplate.opsForValue().get(ticketKey);
        if (rawTicket == null) {
            throw new BusinessException(ErrorCode.EXPIRED_2FA_TICKET);
        }

        TicketData currentTicket = deserializeTicket(rawTicket);
        String newOtp = generateOtp();

        saveTicket(preAuthToken, new TicketData(currentTicket.email(), newOtp, 0), TICKET_TTL);
        setCooldown(preAuthToken);

        sendOtpNotification(currentTicket.email(), newOtp);
    }

    /**
     * Lấy email gắn liền với vé 2FA trong Redis (nếu còn hiệu lực)
     * phục vụ cho việc Rate Limiting chuẩn xác theo tài khoản.
     */
    public String getTicketEmail(@NonNull String preAuthToken) {
        String ticketKey = ticketKey(preAuthToken);
        String rawTicket = redisTemplate.opsForValue().get(ticketKey);
        if (rawTicket == null) {
            return null;
        }
        try {
            TicketData ticket = deserializeTicket(rawTicket);
            return ticket.email();
        } catch (Exception e) {
            return null;
        }
    }

    private void sendOtpNotification(String email, String otp) {
        boolean isDev = isDevEnvironment();
        try {
            smtpEmailService.sendAdmin2faOtpEmail(email, otp);
        } catch (Exception e) {
            log.error("Failed to dispatch 2FA email to {}", email, e);
            if (!isDev) {
                throw new BusinessException(
                        ErrorCode.DEPENDENCY_UNAVAILABLE, "Unable to send 2FA OTP email. Please try again later.");
            }
        }
        if (isDev) {
            log.warn("[DEV ONLY / 2FA] OTP for admin account <{}> is: {}", email, otp);
        }
    }

    private boolean isDevEnvironment() {
        return environment != null && environment.acceptsProfiles(Profiles.of("dev", "local", "test"));
    }

    private String generateOtp() {
        int code = secureRandom.nextInt(1_000_000);
        return String.format("%06d", code);
    }

    private void saveTicket(String token, TicketData ticket, Duration ttl) {
        try {
            String json = objectMapper.writeValueAsString(ticket);
            redisTemplate.opsForValue().set(ticketKey(token), json, ttl);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize 2FA ticket", e);
        }
    }

    private TicketData deserializeTicket(String json) {
        try {
            return objectMapper.readValue(json, TicketData.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize 2FA ticket", e);
        }
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
}
