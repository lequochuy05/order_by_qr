package com.qros.modules.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qros.infrastructure.mail.EmailService;
import com.qros.infrastructure.sms.SmsService;
import com.qros.modules.auth.config.PasswordResetProperties;
import com.qros.modules.auth.model.PasswordResetToken;
import com.qros.modules.auth.repository.PasswordResetTokenRepository;
import com.qros.modules.user.model.User;
import com.qros.modules.user.model.enums.UserRole;
import com.qros.modules.user.model.enums.UserStatus;
import com.qros.modules.user.service.UserLookupService;
import com.qros.shared.config.AppProperties;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.ratelimit.ClientAddressResolver;
import com.qros.shared.time.AppTime;
import com.qros.shared.transaction.TransactionSideEffectService;
import com.qros.shared.validation.MaxUtf8Bytes;
import com.qros.shared.validation.MaxUtf8BytesValidator;
import java.lang.annotation.Annotation;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.Environment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthHardeningRegressionTest {

    @Test
    void maxUtf8BytesValidatorRejectsMultibytePasswordExceeding72Bytes() {
        MaxUtf8BytesValidator validator = new MaxUtf8BytesValidator();
        validator.initialize(new MaxUtf8Bytes() {
            @Override
            public int value() {
                return 72;
            }

            @Override
            public String message() {
                return "too many bytes";
            }

            @Override
            public Class<?>[] groups() {
                return new Class[0];
            }

            @Override
            public Class<? extends jakarta.validation.Payload>[] payload() {
                return new Class[0];
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return MaxUtf8Bytes.class;
            }
        });

        // 25 Vietnamese 3-byte characters = 75 UTF-8 bytes (> 72), even though String.length() == 25
        String multibyte75Bytes = "ệ".repeat(25);
        assertThat(multibyte75Bytes.length()).isEqualTo(25);
        assertThat(validator.isValid(multibyte75Bytes, null)).isFalse();
        assertThat(validator.isValid("A".repeat(72), null)).isTrue();
    }

    @Test
    void passwordResetWithOtpRecordsFailedAttemptAndRejectsExhaustedToken() {
        UserLookupService userRepo = mock(UserLookupService.class);
        PasswordResetTokenRepository tokenRepo = mock(PasswordResetTokenRepository.class);
        PasswordResetAttemptRecorder attemptRecorder = mock(PasswordResetAttemptRecorder.class);
        PasswordResetProperties props = new PasswordResetProperties();
        props.setPhoneEnabled(true);

        PasswordResetService service = new PasswordResetService(
                userRepo,
                tokenRepo,
                mock(EmailService.class),
                mock(SmsService.class),
                props,
                mock(Environment.class),
                mock(PasswordEncoder.class),
                mock(TransactionSideEffectService.class),
                mock(RefreshTokenService.class),
                mock(AccountLockoutService.class),
                mock(ApplicationEventPublisher.class),
                attemptRecorder);

        User user = User.builder()
                .id(10L)
                .email("staff@qros.vn")
                .phone("0901234567")
                .role(UserRole.STAFF)
                .status(UserStatus.ACTIVE)
                .build();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .id(42L)
                .user(user)
                .token("tok-42")
                .otpCode("hashed-valid-otp")
                .expiryDate(AppTime.now().plusMinutes(10))
                .used(false)
                .attempts(0)
                .build();

        when(tokenRepo.findLatestOtpForUpdate(eq("0901234567"), any(), eq(PasswordResetToken.Via.PHONE), any()))
                .thenReturn(List.of(resetToken));

        assertThatThrownBy(() -> service.resetPasswordWithOtp("0901234567", "000000", "NewPass@123"))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PASSWORD_RESET_TOKEN_INVALID));

        verify(attemptRecorder).recordFailedOtpAttempt(42L);
    }

    @Test
    void clientAddressResolverIgnoresSpoofedForwardedHeaderFromPublicIpAndTrustsCgnatProxy() {
        AppProperties props = new AppProperties();
        props.getSecurity().setTrustProxyHeaders(true);
        ClientAddressResolver resolver = new ClientAddressResolver(props);

        MockHttpServletRequest spoofed = new MockHttpServletRequest();
        spoofed.setRemoteAddr("198.51.100.25");
        spoofed.addHeader("X-Forwarded-For", "1.2.3.4");
        assertThat(resolver.resolve(spoofed)).isEqualTo("198.51.100.25");

        MockHttpServletRequest behindRenderProxy = new MockHttpServletRequest();
        behindRenderProxy.setRemoteAddr("100.64.12.8");
        behindRenderProxy.addHeader("X-Forwarded-For", "203.0.113.99, 100.64.12.8");
        assertThat(resolver.resolve(behindRenderProxy)).isEqualTo("203.0.113.99");
    }
}
