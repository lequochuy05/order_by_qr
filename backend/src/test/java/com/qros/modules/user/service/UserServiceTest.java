package com.qros.modules.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qros.modules.auth.service.AccountLockoutService;
import com.qros.modules.auth.service.RefreshTokenService;
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

    private static final List<UserRole> ADMIN_ROLES = List.of(UserRole.ADMIN, UserRole.MANAGER);

    private UserRepository userRepository;
    private RefreshTokenService refreshTokenService;
    private AccountLockoutService accountLockoutService;
    private PasswordEncoder passwordEncoder;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        refreshTokenService = mock(RefreshTokenService.class);
        accountLockoutService = mock(AccountLockoutService.class);
        passwordEncoder = mock(PasswordEncoder.class);
        userService = new UserService(
                userRepository,
                passwordEncoder,
                mock(ApplicationEventPublisher.class),
                mock(UserMapper.class),
                refreshTokenService,
                accountLockoutService);
    }

    @Test
    void updateCannotDemoteLastActiveManager() {
        User manager = user(1L, "manager@example.com", UserRole.MANAGER, UserStatus.ACTIVE);
        UpdateUserRequest request =
                new UpdateUserRequest("Manager", manager.getEmail(), null, UserRole.STAFF, UserStatus.ACTIVE);
        when(userRepository.findByRoleInAndStatusForUpdate(ADMIN_ROLES, UserStatus.ACTIVE))
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
        when(userRepository.findByRoleInAndStatusForUpdate(ADMIN_ROLES, UserStatus.ACTIVE))
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
        when(userRepository.findByRoleInAndStatusForUpdate(ADMIN_ROLES, UserStatus.ACTIVE))
                .thenReturn(List.of(manager, otherManager));
        when(userRepository.findById(manager.getId())).thenReturn(Optional.of(manager));

        userService.update(manager.getId(), request);

        verify(userRepository).save(manager);
    }

    @Test
    void deleteRejectsSelfDelete() {
        User manager = user(1L, "manager@example.com", UserRole.MANAGER, UserStatus.ACTIVE);
        when(userRepository.findByRoleInAndStatusForUpdate(ADMIN_ROLES, UserStatus.ACTIVE))
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
        when(userRepository.findByRoleInAndStatusForUpdate(ADMIN_ROLES, UserStatus.ACTIVE))
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

    @Test
    void updateRevokesTokensWhenUserDeactivated() {
        User staff = user(5L, "staff@example.com", UserRole.STAFF, UserStatus.ACTIVE);
        UpdateUserRequest request =
                new UpdateUserRequest("Staff", staff.getEmail(), null, UserRole.STAFF, UserStatus.INACTIVE);
        when(userRepository.findById(staff.getId())).thenReturn(Optional.of(staff));

        userService.update(staff.getId(), request);

        verify(refreshTokenService).revokeUserTokens(staff.getId());
    }

    @Test
    void updateRevokesTokensWhenUserRoleChanged() {
        User staff = user(5L, "staff@example.com", UserRole.STAFF, UserStatus.ACTIVE);
        UpdateUserRequest request =
                new UpdateUserRequest("Staff", staff.getEmail(), null, UserRole.CHEF, UserStatus.ACTIVE);
        when(userRepository.findById(staff.getId())).thenReturn(Optional.of(staff));

        userService.update(staff.getId(), request);

        verify(refreshTokenService).revokeUserTokens(staff.getId());
    }

    @Test
    void deleteRevokesTokensForTargetUser() {
        User manager = user(1L, "manager@example.com", UserRole.MANAGER, UserStatus.ACTIVE);
        User staff = user(5L, "staff@example.com", UserRole.STAFF, UserStatus.ACTIVE);
        when(userRepository.findByEmailIgnoreCase(manager.getEmail())).thenReturn(Optional.of(manager));
        when(userRepository.findById(staff.getId())).thenReturn(Optional.of(staff));

        userService.delete(staff.getId(), manager.getEmail());

        verify(userRepository).delete(staff);
        verify(refreshTokenService).revokeUserTokens(staff.getId());
    }

    @Test
    void resetPasswordRevokesTokensForUserAndUnlocksAccount() {
        User staff = user(5L, "staff@example.com", UserRole.STAFF, UserStatus.ACTIVE);
        when(userRepository.findById(staff.getId())).thenReturn(Optional.of(staff));
        when(passwordEncoder.encode("new-secret")).thenReturn("encoded-secret");

        userService.resetPassword(staff.getId(), "new-secret");

        verify(refreshTokenService).revokeUserTokens(staff.getId());
        verify(accountLockoutService).unlock(staff.getEmail());
        assertThat(staff.isMustChangePassword()).isTrue();
    }

    @Test
    void managerCannotCreateAdminOrManager() {
        User managerActor = user(10L, "mgr@example.com", UserRole.MANAGER, UserStatus.ACTIVE);
        when(userRepository.findByEmailIgnoreCase(managerActor.getEmail())).thenReturn(Optional.of(managerActor));

        com.qros.modules.user.dto.request.CreateUserRequest createAdminReq =
                new com.qros.modules.user.dto.request.CreateUserRequest(
                        "New Admin", "admin2@example.com", null, "Secret123!", UserRole.ADMIN, UserStatus.ACTIVE);

        assertThatThrownBy(() -> userService.create(createAdminReq, managerActor.getEmail()))
                .isInstanceOfSatisfying(BusinessException.class, ex -> assertThat(ex.getErrorCode())
                        .isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void managerCannotDeleteAdminOrManager() {
        User managerActor = user(10L, "mgr@example.com", UserRole.MANAGER, UserStatus.ACTIVE);
        User targetAdmin = user(1L, "admin@example.com", UserRole.ADMIN, UserStatus.ACTIVE);
        when(userRepository.findByEmailIgnoreCase(managerActor.getEmail())).thenReturn(Optional.of(managerActor));
        when(userRepository.findById(targetAdmin.getId())).thenReturn(Optional.of(targetAdmin));
        when(userRepository.findByRoleInAndStatusForUpdate(ADMIN_ROLES, UserStatus.ACTIVE))
                .thenReturn(List.of(targetAdmin, managerActor));

        assertThatThrownBy(() -> userService.delete(targetAdmin.getId(), managerActor.getEmail()))
                .isInstanceOfSatisfying(BusinessException.class, ex -> assertThat(ex.getErrorCode())
                        .isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void managerCannotResetPasswordForAdmin() {
        User managerActor = user(10L, "mgr@example.com", UserRole.MANAGER, UserStatus.ACTIVE);
        User targetAdmin = user(1L, "admin@example.com", UserRole.ADMIN, UserStatus.ACTIVE);
        when(userRepository.findByEmailIgnoreCase(managerActor.getEmail())).thenReturn(Optional.of(managerActor));
        when(userRepository.findById(targetAdmin.getId())).thenReturn(Optional.of(targetAdmin));

        assertThatThrownBy(() -> userService.resetPassword(targetAdmin.getId(), "new-pass", managerActor.getEmail()))
                .isInstanceOfSatisfying(BusinessException.class, ex -> assertThat(ex.getErrorCode())
                        .isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void managerCannotUpdateAdminOrPromoteToAdmin() {
        User managerActor = user(10L, "mgr@example.com", UserRole.MANAGER, UserStatus.ACTIVE);
        User targetAdmin = user(1L, "admin@example.com", UserRole.ADMIN, UserStatus.ACTIVE);
        User targetStaff = user(5L, "staff@example.com", UserRole.STAFF, UserStatus.ACTIVE);
        when(userRepository.findByEmailIgnoreCase(managerActor.getEmail())).thenReturn(Optional.of(managerActor));
        when(userRepository.findById(targetAdmin.getId())).thenReturn(Optional.of(targetAdmin));
        when(userRepository.findById(targetStaff.getId())).thenReturn(Optional.of(targetStaff));

        UpdateUserRequest updateAdminReq = new UpdateUserRequest(
                "Admin Modified", targetAdmin.getEmail(), null, UserRole.ADMIN, UserStatus.ACTIVE);
        assertThatThrownBy(() -> userService.update(targetAdmin.getId(), updateAdminReq, managerActor.getEmail()))
                .isInstanceOfSatisfying(BusinessException.class, ex -> assertThat(ex.getErrorCode())
                        .isEqualTo(ErrorCode.FORBIDDEN));

        UpdateUserRequest promoteStaffToAdminReq = new UpdateUserRequest(
                "Staff Promoted", targetStaff.getEmail(), null, UserRole.ADMIN, UserStatus.ACTIVE);
        assertThatThrownBy(
                        () -> userService.update(targetStaff.getId(), promoteStaffToAdminReq, managerActor.getEmail()))
                .isInstanceOfSatisfying(BusinessException.class, ex -> assertThat(ex.getErrorCode())
                        .isEqualTo(ErrorCode.FORBIDDEN));
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
