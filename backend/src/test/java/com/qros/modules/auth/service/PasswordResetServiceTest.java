package com.qros.modules.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qros.infrastructure.mail.SmtpEmailService;
import com.qros.modules.auth.config.PasswordResetProperties;
import com.qros.modules.auth.model.PasswordResetToken;
import com.qros.modules.auth.repository.PasswordResetTokenRepository;
import com.qros.modules.user.model.User;
import com.qros.modules.user.repository.UserRepository;
import com.qros.shared.time.AppTime;
import com.qros.shared.transaction.TransactionSideEffectService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
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
                new PasswordResetProperties(),
                mock(Environment.class),
                passwordEncoder,
                mock(TransactionSideEffectService.class));

        service.resetPassword("raw-token", "new-password");

        assertThat(token.isUsed()).isTrue();
        assertThat(user.getPassword()).isEqualTo("encoded-password");
        verify(tokenRepository).findByTokenForUpdate(anyString());
        verify(tokenRepository).save(token);
        verify(userRepository).save(user);
    }
}
