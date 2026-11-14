package com.qros.modules.user.service;

import com.qros.modules.auth.service.AccountLockoutService;
import com.qros.modules.auth.service.RefreshTokenService;
import com.qros.modules.user.dto.request.*;
import com.qros.modules.user.dto.response.UserResponse;
import com.qros.modules.user.mapper.UserMapper;
import com.qros.modules.user.model.User;
import com.qros.modules.user.model.enums.UserRole;
import com.qros.modules.user.model.enums.UserStatus;
import com.qros.modules.user.repository.UserRepository;
import com.qros.shared.cache.CacheNames;
import com.qros.shared.event.DomainEvents.*;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.time.AppTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.NonNull;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * UserService - Core service for user management operations (CRUD).
 */
@Slf4j
@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final UserMapper userMapper;
    private final RefreshTokenService refreshTokenService;
    private final AccountLockoutService accountLockoutService;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            ApplicationEventPublisher eventPublisher,
            UserMapper userMapper,
            RefreshTokenService refreshTokenService,
            AccountLockoutService accountLockoutService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
        this.userMapper = userMapper;
        this.refreshTokenService = refreshTokenService;
        this.accountLockoutService = accountLockoutService;
    }

    @Transactional
    @CacheEvict(value = CacheNames.USERS, allEntries = true)
    public UserResponse create(@NonNull CreateUserRequest req) {
        return create(req, null);
    }

    @Transactional
    @CacheEvict(value = CacheNames.USERS, allEntries = true)
    public UserResponse create(@NonNull CreateUserRequest req, String actorEmail) {
        validateManagerHierarchyRestrictions(actorEmail, req.role(), "create");
        String email = req.email().trim().toLowerCase();
        String phone = StringUtils.hasText(req.phone()) ? req.phone().trim() : null;
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException(ErrorCode.EMAIL_EXISTS);
        }

        if (StringUtils.hasText(phone) && userRepository.existsByPhone(phone)) {
            throw new BusinessException(ErrorCode.PHONE_EXISTS);
        }

        User u = userMapper.toEntity(req);
        u.setEmail(email);
        u.setPhone(phone);
        u.setPassword(passwordEncoder.encode(req.password()));
        u.setPasswordChangedAt(AppTime.now().truncatedTo(ChronoUnit.SECONDS));
        u.setMustChangePassword(true);
        u.setStatus(req.status());
        u.setRole(req.role());

        userRepository.save(u);
        // log.info("New user created: {}", u.getEmail());
        eventPublisher.publishEvent(new UserChangeEvent());
        return userMapper.toDto(u);
    }

    private static final List<UserRole> ADMIN_ROLES = List.of(UserRole.ADMIN, UserRole.MANAGER);

    @Transactional
    @CacheEvict(value = CacheNames.USERS, allEntries = true)
    public UserResponse update(@NonNull Long id, @NonNull UpdateUserRequest req) {
        return update(id, req, null);
    }

    @Transactional
    @CacheEvict(value = CacheNames.USERS, allEntries = true)
    public UserResponse update(@NonNull Long id, @NonNull UpdateUserRequest req, String actorEmail) {
        User u = userRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        validateManagerHierarchyRestrictions(actorEmail, u.getRole(), "update");
        if (req.role() != null) {
            validateManagerHierarchyRestrictions(actorEmail, req.role(), "assign role to");
        }

        String email = req.email().trim().toLowerCase();
        String phone = StringUtils.hasText(req.phone()) ? req.phone().trim() : null;
        List<User> activeManagers = mayRemoveActiveManager(req)
                ? userRepository.findByRoleInAndStatusForUpdate(ADMIN_ROLES, UserStatus.ACTIVE)
                : List.of();

        UserRole nextRole = req.role() != null ? req.role() : u.getRole();
        UserStatus nextStatus = req.status() != null ? req.status() : u.getStatus();
        if (isActiveManager(u) && !isActiveManager(nextRole, nextStatus) && activeManagers.size() <= 1) {
            throw new BusinessException(ErrorCode.LAST_ACTIVE_MANAGER_REQUIRED);
        }

        if (!u.getEmail().equalsIgnoreCase(email) && userRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new BusinessException(ErrorCode.EMAIL_EXISTS);
        }

        if (StringUtils.hasText(phone)
                && !Objects.equals(u.getPhone(), phone)
                && userRepository.existsByPhoneAndIdNot(phone, id)) {
            throw new BusinessException(ErrorCode.PHONE_EXISTS);
        }

        boolean emailChanged = !u.getEmail().equalsIgnoreCase(email);
        boolean statusDeactivated = req.status() != null && req.status() != UserStatus.ACTIVE;
        boolean roleChanged = req.role() != null && req.role() != u.getRole();

        userMapper.updateEntity(u, req);
        u.setEmail(email);
        u.setPhone(phone);

        if (req.role() != null) {
            u.setRole(req.role());
        }
        if (req.status() != null) {
            u.setStatus(req.status());
        }

        userRepository.save(u);
        if (statusDeactivated || roleChanged || emailChanged) {
            refreshTokenService.revokeUserTokens(u.getId());
        }
        // log.info("User updated: {}", u.getEmail());
        eventPublisher.publishEvent(new UserChangeEvent());
        return userMapper.toDto(u);
    }

    @Transactional
    @CacheEvict(value = CacheNames.USERS, allEntries = true)
    public void delete(@NonNull Long id, @NonNull String actorEmail) {
        List<User> activeManagers = userRepository.findByRoleInAndStatusForUpdate(ADMIN_ROLES, UserStatus.ACTIVE);
        User actor = userRepository
                .findByEmailIgnoreCase(actorEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        User target = userRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (Objects.equals(actor.getId(), target.getId())) {
            throw new BusinessException(ErrorCode.SELF_DELETE_NOT_ALLOWED);
        }
        if (actor.getRole() == UserRole.MANAGER
                && (target.getRole() == UserRole.ADMIN || target.getRole() == UserRole.MANAGER)) {
            throw new BusinessException(
                    ErrorCode.FORBIDDEN, "Manager is not allowed to delete Admin or Manager accounts");
        }
        if (isActiveManager(target) && activeManagers.size() <= 1) {
            throw new BusinessException(ErrorCode.LAST_ACTIVE_MANAGER_REQUIRED);
        }

        userRepository.delete(target);
        refreshTokenService.revokeUserTokens(target.getId());
        // log.info("User with id {} deleted", id);
        eventPublisher.publishEvent(new UserChangeEvent());
    }

    @Transactional
    public void resetPassword(@NonNull Long id, @NonNull String newPassword) {
        resetPassword(id, newPassword, null);
    }

    @Transactional
    public void resetPassword(@NonNull Long id, @NonNull String newPassword, String actorEmail) {
        User u = userRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        validateManagerHierarchyRestrictions(actorEmail, u.getRole(), "reset password for");

        if (!StringUtils.hasText(newPassword)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "New password cannot be empty");
        }

        u.setPassword(passwordEncoder.encode(newPassword));
        u.setPasswordChangedAt(AppTime.now().truncatedTo(ChronoUnit.SECONDS));
        u.setMustChangePassword(true);
        userRepository.save(u);
        refreshTokenService.revokeUserTokens(u.getId());
        accountLockoutService.unlock(u.getEmail());
        // log.info("Password reset for user: {}", u.getEmail());
    }

    private void validateManagerHierarchyRestrictions(String actorEmail, UserRole targetRole, String action) {
        if (!StringUtils.hasText(actorEmail) || targetRole == null) {
            return;
        }
        User actor = userRepository.findByEmailIgnoreCase(actorEmail).orElse(null);
        if (actor != null && actor.getRole() == UserRole.MANAGER) {
            if (targetRole == UserRole.ADMIN || targetRole == UserRole.MANAGER) {
                throw new BusinessException(
                        ErrorCode.FORBIDDEN, "Manager is not allowed to " + action + " Admin or Manager accounts");
            }
        }
    }

    private boolean mayRemoveActiveManager(UpdateUserRequest req) {
        return (req.role() != null && !ADMIN_ROLES.contains(req.role()))
                || (req.status() != null && req.status() != UserStatus.ACTIVE);
    }

    private boolean isActiveManager(User user) {
        return isActiveManager(user.getRole(), user.getStatus());
    }

    private boolean isActiveManager(UserRole role, UserStatus status) {
        return role != null && ADMIN_ROLES.contains(role) && status == UserStatus.ACTIVE;
    }
}
