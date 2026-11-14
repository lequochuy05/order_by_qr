package com.qros.modules.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qros.modules.auth.service.AccountLockoutService;
import com.qros.modules.auth.service.RefreshTokenService;
import com.qros.modules.user.dto.request.PasswordChangeRequest;
import com.qros.modules.user.mapper.UserMapper;
import com.qros.modules.user.model.User;
import com.qros.modules.user.repository.UserRepository;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

class CurrentUserServiceTest {

    private UserRepository userRepository;
    private UserMapper userMapper;
    private ApplicationEventPublisher eventPublisher;
    private PasswordEncoder passwordEncoder;
    private RefreshTokenService refreshTokenService;
    private AccountLockoutService accountLockoutService;
    private CurrentUserService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userMapper = mock(UserMapper.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        passwordEncoder = mock(PasswordEncoder.class);
        refreshTokenService = mock(RefreshTokenService.class);
        accountLockoutService = mock(AccountLockoutService.class);

        service = new CurrentUserService(
                userRepository,
                userMapper,
                eventPublisher,
                passwordEncoder,
                refreshTokenService,
                accountLockoutService);
    }

    @Test
    void changeCurrentPasswordSuccessUpdatesPasswordRevokesTokensAndUnlocks() {
        String email = "chef@example.com";
        User user = User.builder()
                .id(9L)
                .email(email)
                .password("encoded-old-pass")
                .mustChangePassword(true)
                .build();

        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPass123", "encoded-old-pass")).thenReturn(true);
        when(passwordEncoder.encode("NewPass123")).thenReturn("encoded-new-pass");

        PasswordChangeRequest request = new PasswordChangeRequest("OldPass123", "NewPass123");

        service.changeCurrentPassword(email, request);

        assertThat(user.getPassword()).isEqualTo("encoded-new-pass");
        assertThat(user.getPasswordChangedAt()).isNotNull();
        assertThat(user.isMustChangePassword()).isFalse();

        verify(userRepository).save(user);
        verify(refreshTokenService).revokeUserTokens(9L);
        verify(accountLockoutService).unlock(email);
    }

    @Test
    void changeCurrentPasswordWithInvalidPasswordThrowsException() {
        String email = "chef@example.com";
        User user =
                User.builder().id(9L).email(email).password("encoded-old-pass").build();

        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPass", "encoded-old-pass")).thenReturn(false);

        PasswordChangeRequest request = new PasswordChangeRequest("WrongPass", "NewPass123");

        assertThatThrownBy(() -> service.changeCurrentPassword(email, request))
                .isInstanceOfSatisfying(BusinessException.class, ex -> assertThat(ex.getErrorCode())
                        .isEqualTo(ErrorCode.PASSWORD_INVALID));

        verify(userRepository, never()).save(user);
        verify(refreshTokenService, never()).revokeUserTokens(anyLong());
    }
}
