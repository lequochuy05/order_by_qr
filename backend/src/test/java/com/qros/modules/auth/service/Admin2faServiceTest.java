package com.qros.modules.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qros.infrastructure.mail.SmtpEmailService;
import com.qros.modules.user.model.User;
import com.qros.modules.user.model.enums.UserRole;
import com.qros.modules.user.model.enums.UserStatus;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class Admin2faServiceTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private SmtpEmailService smtpEmailService;
    private ObjectMapper objectMapper;
    private Admin2faService admin2faService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        smtpEmailService = mock(SmtpEmailService.class);
        objectMapper = new ObjectMapper();

        admin2faService = new Admin2faService(redisTemplate, smtpEmailService, objectMapper);
    }

    @Test
    void initiate2faStoresTicketAndSetsCooldown() {
        User admin = new User();
        admin.setId(1L);
        admin.setEmail("admin@qros.vn");
        admin.setRole(UserRole.ADMIN);
        admin.setStatus(UserStatus.ACTIVE);

        String token = admin2faService.initiate2fa(admin);

        assertThat(token).isNotBlank();
        verify(valueOperations).set(eq("qros:2fa:ticket:" + token), anyString(), eq(Duration.ofMinutes(5)));
        verify(valueOperations).set(eq("qros:2fa:cooldown:" + token), eq("1"), eq(Duration.ofSeconds(60)));
        verify(smtpEmailService).sendAdmin2faOtpEmail(eq("admin@qros.vn"), anyString());
    }

    @Test
    void verify2faWhenTicketExpiredThrowsExpiredTicket() {
        when(valueOperations.get("qros:2fa:ticket:expired-token")).thenReturn(null);

        assertThatThrownBy(() -> admin2faService.verify2fa("expired-token", "123456"))
                .isInstanceOfSatisfying(BusinessException.class, ex -> assertThat(ex.getErrorCode())
                        .isEqualTo(ErrorCode.EXPIRED_2FA_TICKET));
    }

    @Test
    void verify2faWithValidOtpReturnsEmailAndCleansUp() {
        String token = "valid-ticket-123";
        String ticketJson =
                """
                {"email":"admin@qros.vn","otp":"654321","attempts":0}
                """;
        when(valueOperations.get("qros:2fa:ticket:" + token)).thenReturn(ticketJson);

        String email = admin2faService.verify2fa(token, "654321");

        assertThat(email).isEqualTo("admin@qros.vn");
        verify(redisTemplate).delete("qros:2fa:ticket:" + token);
        verify(redisTemplate).delete("qros:2fa:cooldown:" + token);
    }

    @Test
    void verify2faWithWrongOtpIncrementsAttemptsAndThrowsInvalidCode() {
        String token = "wrong-otp-token";
        String ticketJson =
                """
                {"email":"admin@qros.vn","otp":"654321","attempts":1}
                """;
        when(valueOperations.get("qros:2fa:ticket:" + token)).thenReturn(ticketJson);
        when(redisTemplate.getExpire(eq("qros:2fa:ticket:" + token), eq(TimeUnit.SECONDS)))
                .thenReturn(200L);

        assertThatThrownBy(() -> admin2faService.verify2fa(token, "000000"))
                .isInstanceOfSatisfying(BusinessException.class, ex -> assertThat(ex.getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_2FA_CODE));

        verify(valueOperations).set(eq("qros:2fa:ticket:" + token), anyString(), eq(Duration.ofSeconds(200)));
    }

    @Test
    void verify2faWhenMaxAttemptsReachedThrowsTooManyAttemptsAndDeletes() {
        String token = "max-attempts-token";
        String ticketJson =
                """
                {"email":"admin@qros.vn","otp":"654321","attempts":4}
                """;
        when(valueOperations.get("qros:2fa:ticket:" + token)).thenReturn(ticketJson);
        when(redisTemplate.getExpire(eq("qros:2fa:ticket:" + token), eq(TimeUnit.SECONDS)))
                .thenReturn(100L);

        assertThatThrownBy(() -> admin2faService.verify2fa(token, "000000"))
                .isInstanceOfSatisfying(BusinessException.class, ex -> assertThat(ex.getErrorCode())
                        .isEqualTo(ErrorCode.TOO_MANY_2FA_ATTEMPTS));

        verify(redisTemplate).delete("qros:2fa:ticket:" + token);
        verify(redisTemplate).delete("qros:2fa:cooldown:" + token);
    }

    @Test
    void resend2faDuringCooldownThrowsCooldownError() {
        String token = "cooldown-active-token";
        when(redisTemplate.hasKey("qros:2fa:cooldown:" + token)).thenReturn(true);

        assertThatThrownBy(() -> admin2faService.resend2fa(token))
                .isInstanceOfSatisfying(BusinessException.class, ex -> assertThat(ex.getErrorCode())
                        .isEqualTo(ErrorCode.COOLDOWN_2FA));
    }

    @Test
    void resend2faSuccessUpdatesTicketAndResetsCooldown() {
        String token = "resend-valid-token";
        String ticketJson =
                """
                {"email":"admin@qros.vn","otp":"111111","attempts":2}
                """;
        when(redisTemplate.hasKey("qros:2fa:cooldown:" + token)).thenReturn(false);
        when(valueOperations.get("qros:2fa:ticket:" + token)).thenReturn(ticketJson);

        admin2faService.resend2fa(token);

        verify(valueOperations).set(eq("qros:2fa:ticket:" + token), anyString(), eq(Duration.ofMinutes(5)));
        verify(valueOperations).set(eq("qros:2fa:cooldown:" + token), eq("1"), eq(Duration.ofSeconds(60)));
        verify(smtpEmailService).sendAdmin2faOtpEmail(eq("admin@qros.vn"), anyString());
    }

    @Test
    void getTicketEmailReturnsEmailWhenTicketExists() {
        String token = "existing-ticket";
        String ticketJson =
                """
                {"email":"admin@qros.vn","otp":"123456","attempts":0}
                """;
        when(valueOperations.get("qros:2fa:ticket:" + token)).thenReturn(ticketJson);

        String email = admin2faService.getTicketEmail(token);

        assertThat(email).isEqualTo("admin@qros.vn");
    }

    @Test
    void getTicketEmailReturnsNullWhenTicketMissing() {
        when(valueOperations.get("qros:2fa:ticket:missing-token")).thenReturn(null);

        String email = admin2faService.getTicketEmail("missing-token");

        assertThat(email).isNull();
    }
}
