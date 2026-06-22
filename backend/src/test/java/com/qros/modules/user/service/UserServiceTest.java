package com.qros.modules.user.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qros.modules.user.dto.request.UpdateUserRequest;
import com.qros.modules.user.mapper.UserMapper;
import com.qros.modules.user.model.User;
import com.qros.modules.user.model.enums.UserRole;
import com.qros.modules.user.model.enums.UserStatus;
import com.qros.modules.user.repository.UserRepository;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserServiceTest {

    private UserRepository userRepository;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userService = new UserService(
                userRepository,
                mock(PasswordEncoder.class),
                mock(ApplicationEventPublisher.class),
                mock(UserMapper.class));
    }

    @Test
    void updateCannotDemoteLastActiveManager() {
        User manager = user(1L, "manager@example.com", UserRole.MANAGER, UserStatus.ACTIVE);
        UpdateUserRequest request =
                new UpdateUserRequest("Manager", manager.getEmail(), null, UserRole.STAFF, UserStatus.ACTIVE);
        when(userRepository.findByRoleAndStatusForUpdate(UserRole.MANAGER, UserStatus.ACTIVE))
                .thenReturn(List.of(manager));
        when(userRepository.findById(manager.getId())).thenReturn(Optional.of(manager));

        assertThatThrownBy(() -> userService.update(manager.getId(), request))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> org.assertj.core.api.Assertions.assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.LAST_ACTIVE_MANAGER_REQUIRED));

        verify(userRepository, never()).save(manager);
    }

    @Test
    void updateCannotDeactivateLastActiveManager() {
        User manager = user(1L, "manager@example.com", UserRole.MANAGER, UserStatus.ACTIVE);
        UpdateUserRequest request =
                new UpdateUserRequest("Manager", manager.getEmail(), null, UserRole.MANAGER, UserStatus.INACTIVE);
        when(userRepository.findByRoleAndStatusForUpdate(UserRole.MANAGER, UserStatus.ACTIVE))
                .thenReturn(List.of(manager));
        when(userRepository.findById(manager.getId())).thenReturn(Optional.of(manager));

        assertThatThrownBy(() -> userService.update(manager.getId(), request))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> org.assertj.core.api.Assertions.assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.LAST_ACTIVE_MANAGER_REQUIRED));

        verify(userRepository, never()).save(manager);
    }

    @Test
    void updateCanDemoteManagerWhenAnotherActiveManagerExists() {
        User manager = user(1L, "manager@example.com", UserRole.MANAGER, UserStatus.ACTIVE);
        User otherManager = user(2L, "other-manager@example.com", UserRole.MANAGER, UserStatus.ACTIVE);
        UpdateUserRequest request =
                new UpdateUserRequest("Manager", manager.getEmail(), null, UserRole.STAFF, UserStatus.ACTIVE);
        when(userRepository.findByRoleAndStatusForUpdate(UserRole.MANAGER, UserStatus.ACTIVE))
                .thenReturn(List.of(manager, otherManager));
        when(userRepository.findById(manager.getId())).thenReturn(Optional.of(manager));

        userService.update(manager.getId(), request);

        verify(userRepository).save(manager);
    }

    @Test
    void deleteRejectsSelfDelete() {
        User manager = user(1L, "manager@example.com", UserRole.MANAGER, UserStatus.ACTIVE);
        when(userRepository.findByRoleAndStatusForUpdate(UserRole.MANAGER, UserStatus.ACTIVE))
                .thenReturn(List.of(manager));
        when(userRepository.findByEmailIgnoreCase(manager.getEmail())).thenReturn(Optional.of(manager));
        when(userRepository.findById(manager.getId())).thenReturn(Optional.of(manager));

        assertThatThrownBy(() -> userService.delete(manager.getId(), manager.getEmail()))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> org.assertj.core.api.Assertions.assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.SELF_DELETE_NOT_ALLOWED));

        verify(userRepository, never()).delete(manager);
    }

    @Test
    void deleteRejectsLastActiveManager() {
        User actor = user(2L, "staff@example.com", UserRole.STAFF, UserStatus.ACTIVE);
        User manager = user(1L, "manager@example.com", UserRole.MANAGER, UserStatus.ACTIVE);
        when(userRepository.findByRoleAndStatusForUpdate(UserRole.MANAGER, UserStatus.ACTIVE))
                .thenReturn(List.of(manager));
        when(userRepository.findByEmailIgnoreCase(actor.getEmail())).thenReturn(Optional.of(actor));
        when(userRepository.findById(manager.getId())).thenReturn(Optional.of(manager));

        assertThatThrownBy(() -> userService.delete(manager.getId(), actor.getEmail()))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> org.assertj.core.api.Assertions.assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.LAST_ACTIVE_MANAGER_REQUIRED));

        verify(userRepository, never()).delete(manager);
    }

    private User user(Long id, String email, UserRole role, UserStatus status) {
        return User.builder()
                .id(id)
                .email(email)
                .fullName("User")
                .password("password")
                .role(role)
                .status(status)
                .build();
    }
}
