package com.qros.modules.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qros.infrastructure.mail.SmtpEmailService;
import com.qros.infrastructure.sms.SmsService;
import com.qros.modules.auth.config.PasswordResetProperties;
import com.qros.modules.auth.model.PasswordResetToken;
import com.qros.modules.auth.repository.PasswordResetTokenRepository;
import com.qros.modules.user.model.User;
import com.qros.modules.user.repository.UserRepository;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.time.AppTime;
import com.qros.shared.transaction.TransactionSideEffectService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.password.PasswordEncoder;

class PasswordResetServiceTest {

    @Test
    void emailResetConsumesLockedTokenBeforeSavingPassword() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordResetTokenRepository tokenRepository = mock(PasswordResetTokenRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        User user = User.builder()
                .id(7L)
                .email("manager@example.com")
                .password("old-password")
                .build();
        PasswordResetToken token = PasswordResetToken.builder()
                .id(11L)
                .user(user)
                .token("hashed-token")
                .expiryDate(AppTime.now().plusMinutes(5))
                .used(false)
                .build();
        when(tokenRepository.findByTokenForUpdate(anyString())).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("new-password")).thenReturn("encoded-password");

        PasswordResetService service = new PasswordResetService(
                userRepository,
                tokenRepository,
                mock(SmtpEmailService.class),
                mock(SmsService.class),
                new PasswordResetProperties(),
                mock(Environment.class),
                passwordEncoder,
                mock(TransactionSideEffectService.class));

        service.resetPassword("raw-token", "new-password");

        assertThat(token.isUsed()).isTrue();
        assertThat(user.getPassword()).isEqualTo("encoded-password");
        assertThat(user.getPasswordChangedAt()).isNotNull();
        verify(tokenRepository).findByTokenForUpdate(anyString());
        verify(tokenRepository).save(token);
        verify(userRepository).save(user);
    }

    @Test
    void phoneResetRegistersSmsAfterCommitWhenGatewayIsAvailable() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordResetTokenRepository tokenRepository = mock(PasswordResetTokenRepository.class);
        SmsService smsService = mock(SmsService.class);
        Environment environment = mock(Environment.class);
        TransactionSideEffectService sideEffects = mock(TransactionSideEffectService.class);
        PasswordResetProperties properties = new PasswordResetProperties();
        properties.setPhoneEnabled(true);
        User user = User.builder().id(7L).phone("0912345678").build();

        when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(false);
        when(userRepository.findByPhone("0912345678")).thenReturn(Optional.of(user));
        when(smsService.isAvailable()).thenReturn(true);

        PasswordResetService service = new PasswordResetService(
                userRepository,
                tokenRepository,
                mock(SmtpEmailService.class),
                smsService,
                properties,
                environment,
                mock(PasswordEncoder.class),
                sideEffects);

        service.createOtpAndSendOtp("0912345678");

        ArgumentCaptor<Runnable> sendOtp = ArgumentCaptor.forClass(Runnable.class);
        verify(sideEffects).afterCommit(sendOtp.capture(), eq("send password reset OTP SMS"));

        sendOtp.getValue().run();
        verify(smsService).sendOtp(eq("+84912345678"), matches("\\d{6}"));
    }

    @Test
    void phoneResetFailsBeforeCreatingTokenWhenSmsIsUnavailable() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordResetTokenRepository tokenRepository = mock(PasswordResetTokenRepository.class);
        SmsService smsService = mock(SmsService.class);
        Environment environment = mock(Environment.class);
        PasswordResetProperties properties = new PasswordResetProperties();
        properties.setPhoneEnabled(true);
        User user = User.builder().id(7L).phone("0912345678").build();

        when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(false);
        when(userRepository.findByPhone("0912345678")).thenReturn(Optional.of(user));
        when(smsService.isAvailable()).thenReturn(false);

        PasswordResetService service = new PasswordResetService(
                userRepository,
                tokenRepository,
                mock(SmtpEmailService.class),
                smsService,
                properties,
                environment,
                mock(PasswordEncoder.class),
                mock(TransactionSideEffectService.class));

        assertThatThrownBy(() -> service.createOtpAndSendOtp("0912345678"))
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.FEATURE_DISABLED));

        verify(tokenRepository, never()).save(any());
    }
}
