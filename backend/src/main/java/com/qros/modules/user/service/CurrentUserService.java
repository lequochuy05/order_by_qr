package com.qros.modules.user.service;

import com.qros.modules.user.dto.request.*;
import com.qros.modules.user.dto.response.UserResponse;
import com.qros.modules.user.mapper.UserMapper;
import com.qros.modules.user.model.User;
import com.qros.modules.user.repository.UserRepository;
import com.qros.shared.cache.CacheNames;
import com.qros.shared.event.DomainEvents.*;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.time.AppTime;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.NonNull;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class CurrentUserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final PasswordEncoder passwordEncoder;

    public UserResponse getCurrentProfile(@NonNull String email) {
        return userRepository
                .findByEmailIgnoreCase(email)
                .map(userMapper::toDto)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Long getCurrentUserId(@NonNull String email) {
        return userRepository
                .findByEmailIgnoreCase(email)
                .map(User::getId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    @Transactional
    @CacheEvict(value = CacheNames.USERS, allEntries = true)
    public UserResponse updateCurrentProfile(@NonNull String email, @NonNull ProfileUpdateRequest req) {
        User u = userRepository
                .findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String nextPhone = StringUtils.hasText(req.phone()) ? req.phone().trim() : null;
        if (StringUtils.hasText(nextPhone)
                && !Objects.equals(u.getPhone(), nextPhone)
                && userRepository.existsByPhoneAndIdNot(nextPhone, u.getId())) {
            throw new BusinessException(ErrorCode.PHONE_EXISTS);
        }

        u.setFullName(req.fullName().trim());
        u.setPhone(nextPhone);

        userRepository.save(u);
        // log.info("User self-updated profile: {}", u.getEmail());
        eventPublisher.publishEvent(new UserChangeEvent());
        return userMapper.toDto(u);
    }

    @Transactional
    public void changeCurrentPassword(@NonNull String email, @NonNull PasswordChangeRequest req) {
        User u = userRepository
                .findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(req.currentPassword(), u.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_INVALID);
        }

        u.setPassword(passwordEncoder.encode(req.newPassword()));
        u.setPasswordChangedAt(AppTime.now());
        userRepository.save(u);
        // log.info("User changed own password: {}", u.getEmail());
    }
}
