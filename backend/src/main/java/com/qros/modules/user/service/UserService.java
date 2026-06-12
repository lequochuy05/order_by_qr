package com.qros.modules.user.service;

import com.qros.modules.notification.service.NotificationService;
import com.qros.modules.user.dto.request.*;
import com.qros.modules.user.dto.response.UserResponse;
import com.qros.modules.user.mapper.UserMapper;
import com.qros.modules.user.model.User;
import com.qros.modules.user.repository.UserRepository;
import com.qros.shared.cache.CacheNames;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.lang.NonNull;

import java.util.Objects;
/**
 * UserService - Core service for user management operations (CRUD).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;
    private final UserMapper userMapper;

    @Transactional
    @CacheEvict(value = CacheNames.USERS, allEntries = true)
    public UserResponse create(@NonNull CreateUserRequest req) {
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
        u.setStatus(req.status());
        u.setRole(req.role());

        userRepository.save(u);
        // log.info("New user created: {}", u.getEmail());
        notificationService.notifyUserChange();
        return userMapper.toDto(u);
    }

    @Transactional
    @CacheEvict(value = CacheNames.USERS, allEntries = true)
    public UserResponse update(@NonNull Long id, @NonNull UpdateUserRequest req) {
        String email = req.email().trim().toLowerCase();
        String phone = StringUtils.hasText(req.phone()) ? req.phone().trim() : null;
        User u = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!u.getEmail().equalsIgnoreCase(email)
                && userRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new BusinessException(ErrorCode.EMAIL_EXISTS);
        }

        if (StringUtils.hasText(phone)
            && !Objects.equals(u.getPhone(), phone)
            && userRepository.existsByPhoneAndIdNot(phone, id)) {
            throw new BusinessException(ErrorCode.PHONE_EXISTS);
        }

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
        // log.info("User updated: {}", u.getEmail());
        notificationService.notifyUserChange();
        return userMapper.toDto(u);
    }

    @Transactional
    @CacheEvict(value = CacheNames.USERS, allEntries = true)
    public void delete(@NonNull Long id) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        userRepository.delete(u);
        // log.info("User with id {} deleted", id);
        notificationService.notifyUserChange();
    }

    @Transactional
    public void resetPassword(@NonNull Long id, @NonNull String newPassword) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!StringUtils.hasText(newPassword)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "New password cannot be empty");
        }

        u.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(u);
        // log.info("Password reset for user: {}", u.getEmail());
    }
}
