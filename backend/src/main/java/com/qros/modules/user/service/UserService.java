package com.qros.modules.user.service;

import com.qros.infrastructure.storage.CloudinaryStorageService;
import com.qros.modules.notification.service.NotificationService;
import com.qros.shared.transaction.TransactionSideEffectService;
import com.qros.infrastructure.cache.CacheService;
import com.qros.modules.user.dto.*;
import com.qros.modules.menu.dto.*;
import com.qros.modules.order.dto.*;
import com.qros.modules.auth.dto.*;
import com.qros.modules.table.dto.*;
import com.qros.shared.response.ApiResponse;
import com.qros.modules.user.mapper.UserMapper;
import com.qros.modules.user.model.User;
import com.qros.modules.user.repository.UserRepository;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.springframework.lang.NonNull;
import org.springframework.beans.factory.annotation.Value;

/**
 * UserService - Core service for user management, authentication, and security
 * operations.
 * Handles user lifecycle, JWT issuance, and profile management.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CloudinaryStorageService cloudinaryStorageService;
    private final JwtService jwtService;
    private final NotificationService notificationService;
    private final UserMapper userMapper;
    private final TransactionSideEffectService sideEffects;
    private final CacheService cacheService;

    @Value("${security.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    /**
     * Authenticates a user and issues a JWT if credentials are valid.
     * 
     * @param req Login credentials
     * @return AuthResponse containing user details and JWT
     * @throws BusinessException if authentication fails or account is inactive
     */
    public AuthResponse login(@NonNull AuthRequest req) {
        User u = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> {
                    log.warn("Login failed: Email {} not found", req.getEmail());
                    return new BusinessException(ErrorCode.INVALID_CREDENTIALS);
                });

        if (u.getStatus() != User.UserStatus.ACTIVE) {
            log.warn("Login blocked: Account {} is {}", u.getEmail(), u.getStatus());
            throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE);
        }

        if (!passwordEncoder.matches(req.getPassword(), u.getPassword())) {
            log.warn("Login failed: Wrong password for user {}", req.getEmail());
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        log.info("User {} logged in successfully", u.getEmail());
        return buildAuthResponse(u);
    }

    public AuthResponse refreshAccessToken(@NonNull String refreshToken) {
        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        String email = jwtService.extractSubject(refreshToken);
        Long userId = Long.valueOf(jwtService.extractClaim(refreshToken, "uid").toString());
        String jti = Objects.toString(jwtService.extractClaim(refreshToken, "jti"), "");
        String cacheKey = refreshTokenCacheKey(userId, jti);

        if (jti.isBlank() || !cacheService.hasKey(cacheKey)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN, "Refresh token has expired or was revoked");
        }
        cacheService.delete(cacheKey);

        User u = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

        if (u.getStatus() != User.UserStatus.ACTIVE || !u.isEnabled()) {
            throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE);
        }

        return buildAuthResponse(u);
    }

    public String issueRefreshToken(@NonNull AuthResponse auth) {
        String jti = UUID.randomUUID().toString();
        String token = jwtService.generateRefreshToken(auth.getEmail(), Map.of(
                "uid", auth.getUserId(),
                "role", auth.getRole().name(),
                "jti", jti));
        cacheService.set(refreshTokenCacheKey(auth.getUserId(), jti), true, refreshExpirationMs, TimeUnit.MILLISECONDS);
        return token;
    }

    public void revokeRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank() || !jwtService.isValid(refreshToken)) {
            return;
        }
        Object uid = jwtService.extractClaim(refreshToken, "uid");
        Object jti = jwtService.extractClaim(refreshToken, "jti");
        if (uid != null && jti != null) {
            cacheService.delete(refreshTokenCacheKey(Long.valueOf(uid.toString()), jti.toString()));
        }
    }

    /**
     * Retrieves all users currently registered in the system.
     * 
     * @return List of user response DTOs
     */
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a single user by their identifier.
     * 
     * @param id User ID
     * @return UserResponse DTO
     * @throws BusinessException if user not found
     */
    public UserResponse getOne(@NonNull Long id) {
        return userRepository.findById(id)
                .map(userMapper::toDto)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * Retrieves the authenticated user's own profile.
     *
     * @param email Authenticated user's email
     * @return UserResponse DTO
     */
    public UserResponse getCurrentProfile(@NonNull String email) {
        return userRepository.findByEmail(email)
                .map(userMapper::toDto)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * Registers a new user with encrypted password and default roles.
     * 
     * @param req Registration details
     * @return Created user details
     * @throws BusinessException if email or phone is already registered
     */
    @Transactional
    public UserResponse create(@NonNull UserUpsertRequest req) {
        if (userRepository.existsByEmailIgnoreCase(req.getEmail())) {
            throw new BusinessException(ErrorCode.EMAIL_EXISTS);
        }

        if (StringUtils.hasText(req.getPhone()) && userRepository.existsByPhone(req.getPhone())) {
            throw new BusinessException(ErrorCode.PHONE_EXISTS);
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
     * Updates an existing user's profile and credentials.
     * 
     * @param id  User ID
     * @param req Update details
     * @return Updated user details
     */
    @Transactional
    public UserResponse update(@NonNull Long id, @NonNull UserUpsertRequest req) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!u.getEmail().equalsIgnoreCase(req.getEmail())
                && userRepository.existsByEmailIgnoreCaseAndIdNot(req.getEmail(), id)) {
            throw new BusinessException(ErrorCode.EMAIL_EXISTS);
        }

        if (StringUtils.hasText(req.getPhone()) && !Objects.equals(u.getPhone(), req.getPhone())
                && userRepository.existsByPhoneAndIdNot(req.getPhone(), id)) {
            throw new BusinessException(ErrorCode.PHONE_EXISTS);
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
     * Updates the authenticated user's editable profile fields only.
     *
     * @param email Authenticated user's email
     * @param req   Profile update payload
     * @return Updated user profile
     */
    @Transactional
    public UserResponse updateCurrentProfile(@NonNull String email, @NonNull ProfileUpdateRequest req) {
        User u = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String nextPhone = StringUtils.hasText(req.getPhone()) ? req.getPhone().trim() : null;
        if (StringUtils.hasText(nextPhone) && !Objects.equals(u.getPhone(), nextPhone)
                && userRepository.existsByPhoneAndIdNot(nextPhone, u.getId())) {
            throw new BusinessException(ErrorCode.PHONE_EXISTS);
        }

        u.setFullName(req.getFullName().trim());
        u.setPhone(nextPhone);

        userRepository.save(u);
        log.info("User self-updated profile: {}", u.getEmail());
        notificationService.notifyUserChange();
        return userMapper.toDto(u);
    }

    /**
     * Soft deletes a user from the system.
     * 
     * @param id User ID
     */
    @Transactional
    public void delete(@NonNull Long id) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        userRepository.delete(u);
        log.info("User with id {} deleted", id);
        notificationService.notifyUserChange();
    }

    /**
     * Resets a user's password with a new provided value.
     * 
     * @param id          User ID
     * @param newPassword Plain text new password
     */
    @Transactional
    public void resetPassword(@NonNull Long id, @NonNull String newPassword) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!StringUtils.hasText(newPassword)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "New password cannot be empty");
        }

        u.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(u);
        log.info("Password reset for user: {}", u.getEmail());
    }

    /**
     * Changes the authenticated user's password after verifying the current
     * password.
     *
     * @param email Authenticated user's email
     * @param req   Password change payload
     */
    @Transactional
    public void changeCurrentPassword(@NonNull String email, @NonNull PasswordChangeRequest req) {
        User u = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(req.getCurrentPassword(), u.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_INVALID);
        }

        u.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(u);
        log.info("User changed own password: {}", u.getEmail());
    }

    /**
     * Uploads a new profile picture for the user and updates their avatar URL.
     * 
     * @param id   User ID
     * @param file The image file
     * @return Updated user details
     */
    @Transactional
    public UserResponse uploadAvatar(@NonNull Long id, @NonNull MultipartFile file) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        try {
            String newUrl = cloudinaryStorageService.upload(file, "order_by_qr/avatars");
            if (StringUtils.hasText(u.getAvatarUrl())) {
                String oldUrl = u.getAvatarUrl();
                sideEffects.afterCommit(() -> cloudinaryStorageService.delete(oldUrl),
                        "delete replaced avatar image for user " + id);
            }
            sideEffects.afterRollback(() -> cloudinaryStorageService.delete(newUrl),
                    "delete rolled back avatar image for user " + id);
            u.setAvatarUrl(newUrl);
            userRepository.save(u);
            log.info("Avatar updated for user: {}", u.getEmail());
            return userMapper.toDto(u);
        } catch (IOException e) {
            log.error("Failed to upload avatar: {}", e.getMessage());
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "Unable to upload avatar", e);
        }
    }

    /**
     * Uploads an avatar for the authenticated user only.
     *
     * @param email Authenticated user's email
     * @param file  The image file
     * @return Updated user profile
     */
    @Transactional
    public UserResponse uploadCurrentAvatar(@NonNull String email, @NonNull MultipartFile file) {
        User u = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return uploadAvatar(u.getId(), file);
    }

    /**
     * Internal helper to construct an AuthResponse with a generated JWT.
     */
    private AuthResponse buildAuthResponse(User u) {
        String token = jwtService.generateAccessToken(u.getEmail(), Map.of(
                "uid", u.getId(),
                "role", u.getRole().name()));
        return new AuthResponse(u.getId(), u.getFullName(), u.getRole(), token, u.getAvatarUrl(), u.getEmail());
    }

    private String refreshTokenCacheKey(Long userId, String jti) {
        return "auth:refresh:" + userId + ":" + jti;
    }
}
