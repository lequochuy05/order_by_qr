package com.qros.modules.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qros.infrastructure.mail.EmailService;
import com.qros.modules.user.model.User;
import com.qros.modules.user.model.enums.UserRole;
import com.qros.modules.user.model.enums.UserStatus;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class Admin2faServiceTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private HashOperations<String, Object, Object> hashOperations;
    private EmailService emailService;
    private ObjectMapper objectMapper;
    private Admin2faService admin2faService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        hashOperations = mock(HashOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        emailService = mock(EmailService.class);
        objectMapper = new JsonMapper();

        admin2faService = new Admin2faService(redisTemplate, emailService, objectMapper);
    }

    @Test
    @SuppressWarnings("unchecked")
    void initiate2faStoresHashedOtpInHashAndSetsCooldown() {
        User admin = new User();
        admin.setId(1L);
        admin.setEmail("admin@qros.vn");
        admin.setRole(UserRole.ADMIN);
        admin.setStatus(UserStatus.ACTIVE);

        String token = admin2faService.initiate2fa(admin);

        assertThat(token).isNotBlank();
        ArgumentCaptor<Map<Object, Object>> hashCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
        verify(hashOperations).putAll(eq("qros:2fa:ticket:" + token), hashCaptor.capture());
        verify(redisTemplate).expire(eq("qros:2fa:ticket:" + token), eq(Duration.ofMinutes(5)));
        verify(valueOperations).set(eq("qros:2fa:cooldown:" + token), eq("1"), eq(Duration.ofSeconds(60)));
        verify(emailService).sendAdmin2faOtpEmail(eq("admin@qros.vn"), otpCaptor.capture());

        String sentOtp = otpCaptor.getValue();
        assertThat(sentOtp).matches("\\d{6}");
        Map<Object, Object> stored = hashCaptor.getValue();
        assertThat(stored.get("email")).isEqualTo("admin@qros.vn");
        assertThat(stored.get("otpHash")).isEqualTo(Admin2faService.hashOtp(sentOtp));
        assertThat(stored.get("otpHash")).isNotEqualTo(sentOtp);
        assertThat(stored.get("attempts")).isEqualTo("0");
        assertThat(stored.get("resends")).isEqualTo("0");
    }

    @Test
    void initiate2faCleansUpTicketAndThrowsDependencyUnavailableWhenMailFails() {
        User admin = new User();
        admin.setId(1L);
        admin.setEmail("admin@qros.vn");
        admin.setRole(UserRole.ADMIN);
        admin.setStatus(UserStatus.ACTIVE);

        doThrow(new RuntimeException("SMTP down")).when(emailService).sendAdmin2faOtpEmail(anyString(), anyString());

        assertThatThrownBy(() -> admin2faService.initiate2fa(admin))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.DEPENDENCY_UNAVAILABLE));

        verify(redisTemplate).delete(org.mockito.ArgumentMatchers.startsWith("qros:2fa:ticket:"));
        verify(redisTemplate).delete(org.mockito.ArgumentMatchers.startsWith("qros:2fa:cooldown:"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void verify2faWhenTicketExpiredThrowsExpiredTicket() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(), any()))
                .thenReturn("EXPIRED");

        assertThatThrownBy(() -> admin2faService.verify2fa("expired-token", "123456"))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.EXPIRED_2FA_TICKET));
    }

    @Test
    @SuppressWarnings("unchecked")
    void verify2faWithValidOtpExecutesAtomicLuaScriptWithSha256Hash() {
        String token = "valid-ticket-123";
        when(redisTemplate.execute(
                        any(RedisScript.class),
                        eq(List.of("qros:2fa:ticket:" + token, "qros:2fa:cooldown:" + token)),
                        eq(Admin2faService.hashOtp("654321")),
                        eq(String.valueOf(Admin2faService.MAX_ATTEMPTS))))
                .thenReturn("OK:admin@qros.vn");

        String email = admin2faService.verify2fa(token, "654321");

        assertThat(email).isEqualTo("admin@qros.vn");
    }

    @Test
    @SuppressWarnings("unchecked")
    void verify2faWithWrongOtpThrowsInvalidCode() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(), any()))
                .thenReturn("INVALID");

        assertThatThrownBy(() -> admin2faService.verify2fa("wrong-otp-token", "000000"))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_2FA_CODE));
    }

    @Test
    @SuppressWarnings("unchecked")
    void verify2faWhenMaxAttemptsReachedThrowsTooManyAttempts() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(), any()))
                .thenReturn("TOO_MANY");

        assertThatThrownBy(() -> admin2faService.verify2fa("max-attempts-token", "000000"))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.TOO_MANY_2FA_ATTEMPTS));
    }

    @Test
    void resend2faDuringCooldownThrowsCooldownError() {
        String token = "cooldown-active-token";
        when(valueOperations.setIfAbsent("qros:2fa:cooldown:" + token, "1", Duration.ofSeconds(60)))
                .thenReturn(false);

        assertThatThrownBy(() -> admin2faService.resend2fa(token))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.COOLDOWN_2FA));
    }

    @Test
    void resend2faSuccessPreservesAttemptsAndFixedExpiryAndIncrementsResends() {
        String token = "resend-valid-token";
        long expiresAt = Instant.now().plusSeconds(180).getEpochSecond();
        when(valueOperations.setIfAbsent("qros:2fa:cooldown:" + token, "1", Duration.ofSeconds(60)))
                .thenReturn(true);
        when(hashOperations.entries("qros:2fa:ticket:" + token))
                .thenReturn(Map.of(
                        "email", "admin@qros.vn",
                        "otpHash", Admin2faService.hashOtp("111111"),
                        "attempts", "2",
                        "resends", "1",
                        "expiresAt", Long.toString(expiresAt)));

        admin2faService.resend2fa(token);

        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(emailService).sendAdmin2faOtpEmail(eq("admin@qros.vn"), otpCaptor.capture());
        verify(hashOperations)
                .put(eq("qros:2fa:ticket:" + token), eq("otpHash"), eq(Admin2faService.hashOtp(otpCaptor.getValue())));
        verify(hashOperations).increment("qros:2fa:ticket:" + token, "resends", 1L);
        verify(redisTemplate).expire(eq("qros:2fa:ticket:" + token), ttlCaptor.capture());
        assertThat(ttlCaptor.getValue().getSeconds()).isBetween(1L, 180L);
    }

    @Test
    void resend2faRejectsWhenResendLimitReached() {
        String token = "max-resend-token";
        long expiresAt = Instant.now().plusSeconds(180).getEpochSecond();
        when(valueOperations.setIfAbsent("qros:2fa:cooldown:" + token, "1", Duration.ofSeconds(60)))
                .thenReturn(true);
        when(hashOperations.entries("qros:2fa:ticket:" + token))
                .thenReturn(Map.of(
                        "email", "admin@qros.vn",
                        "otpHash", Admin2faService.hashOtp("111111"),
                        "attempts", "0",
                        "resends", String.valueOf(Admin2faService.MAX_RESENDS),
                        "expiresAt", Long.toString(expiresAt)));

        assertThatThrownBy(() -> admin2faService.resend2fa(token))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.TOO_MANY_2FA_ATTEMPTS));
        verify(redisTemplate).delete("qros:2fa:cooldown:" + token);
    }

    @Test
    void getTicketEmailReturnsEmailWhenTicketExists() {
        String token = "existing-ticket";
        when(hashOperations.get("qros:2fa:ticket:" + token, "email")).thenReturn("admin@qros.vn");

        String email = admin2faService.getTicketEmail(token);

        assertThat(email).isEqualTo("admin@qros.vn");
    }

    @Test
    void getTicketEmailReturnsNullWhenTicketMissing() {
        when(hashOperations.get("qros:2fa:ticket:missing-token", "email")).thenReturn(null);

        String email = admin2faService.getTicketEmail("missing-token");

        assertThat(email).isNull();
    }
}
