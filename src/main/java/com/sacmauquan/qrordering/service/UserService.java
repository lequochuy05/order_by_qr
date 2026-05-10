package com.sacmauquan.qrordering.service;

import com.sacmauquan.qrordering.dto.*;
import com.sacmauquan.qrordering.mapper.UserMapper;
import com.sacmauquan.qrordering.model.User;
import com.sacmauquan.qrordering.repository.UserRepository;
import com.sacmauquan.qrordering.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.lang.NonNull;

/**
 * UserService - Manage users, authentication and authorization.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ImageManagerService imageManagerService;
    private final JwtService jwtService;
    private final NotificationService notificationService;
    private final UserMapper userMapper;

    /**
     * Login
     */
    public AuthResponse login(@NonNull AuthRequest req) {
        User u = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> {
                    log.warn("Login failed: Email {} not found", req.getEmail());
                    return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid login credentials");
                });

        if (u.getStatus() != User.UserStatus.ACTIVE) {
            log.warn("Login blocked: Account {} is {}", u.getEmail(), u.getStatus());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is locked or not activated");
        }

        if (!passwordEncoder.matches(req.getPassword(), u.getPassword())) {
            log.warn("Login failed: Wrong password for user {}", req.getEmail());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid login credentials");
        }

        log.info("User {} logged in successfully", u.getEmail());
        return buildAuthResponse(u);
    }

    /**
     * Get all users
     */
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Get user by ID
     */
    public UserResponse getOne(@NonNull Long id) {
        return userRepository.findById(id)
                .map(userMapper::toDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    /**
     * Create new user
     */
    @Transactional
    public UserResponse create(@NonNull UserUpsertRequest req) {
        if (userRepository.existsByEmailIncludingDeleted(req.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        if (StringUtils.hasText(req.getPhone()) && userRepository.existsByPhoneIncludingDeleted(req.getPhone())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone number already exists");
        }

        User u = userMapper.toEntity(req);
        u.setPassword(passwordEncoder.encode(req.getPassword()));
        u.setStatus(User.UserStatus.ACTIVE);
        u.setRole(User.Role.STAFF);

        userRepository.save(u);
        log.info("New user created: {}", u.getEmail());
        notificationService.notifyUserChange();
        return userMapper.toDto(u);
    }

    /**
     * Update user
     */
    @Transactional
    public UserResponse update(@NonNull Long id, @NonNull UserUpsertRequest req) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Check email duplicate if changed
        if (!u.getEmail().equals(req.getEmail()) && userRepository.existsByEmailIncludingDeleted(req.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        // Check phone duplicate if changed
        if (StringUtils.hasText(req.getPhone()) && !Objects.equals(u.getPhone(), req.getPhone())
                && userRepository.existsByPhoneIncludingDeleted(req.getPhone())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone number already exists");
        }

        userMapper.updateEntity(u, req);

        if (StringUtils.hasText(req.getPassword())) {
            u.setPassword(passwordEncoder.encode(req.getPassword()));
        }
        if (req.getRole() != null) {
            u.setRole(req.getRole());
        }
        if (StringUtils.hasText(req.getStatus())) {
            u.setStatus(userMapper.mapStatus(req.getStatus()));
        }

        userRepository.save(Objects.requireNonNull(u));
        log.info("User updated: {}", u.getEmail());
        notificationService.notifyUserChange();
        return userMapper.toDto(u);
    }

    /**
     * Delete user
     */
    @Transactional
    public void delete(@NonNull Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        userRepository.deleteById(id);
        log.info("User with id {} deleted", id);
        notificationService.notifyUserChange();
    }

    /**
     * Reset password for user
     */
    @Transactional
    public void resetPassword(@NonNull Long id, @NonNull String newPassword) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!StringUtils.hasText(newPassword)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password cannot be empty");
        }

        u.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(u);
        log.info("Password reset for user: {}", u.getEmail());
    }

    /**
     * Upload and update avatar for user
     */
    @Transactional
    public UserResponse uploadAvatar(@NonNull Long id, @NonNull MultipartFile file) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        try {
            String newUrl = imageManagerService.upload(file, "order_by_qr/avatars");
            if (StringUtils.hasText(u.getAvatarUrl())) {
                imageManagerService.delete(u.getAvatarUrl());
            }
            u.setAvatarUrl(newUrl);
            userRepository.save(u);
            log.info("Avatar updated for user: {}", u.getEmail());
            return userMapper.toDto(u);
        } catch (IOException e) {
            log.error("Failed to upload avatar: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error when upload avatar");
        }
    }

    private AuthResponse buildAuthResponse(User u) {
        String token = jwtService.generateToken(u.getEmail(), Map.of(
                "uid", u.getId(),
                "role", u.getRole().name()));
        return new AuthResponse(u.getId(), u.getFullName(), u.getRole(), token, u.getAvatarUrl());
    }
}
