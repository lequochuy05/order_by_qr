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
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.springframework.lang.NonNull;
import org.springframework.beans.factory.annotation.Value;

/**
 * UserService - Core service for user management, authentication, and security operations.
 * Handles user lifecycle, JWT issuance, and profile management.
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
    private final TransactionSideEffectService sideEffects;
    private final CacheService cacheService;

    @Value("${security.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    /**
     * Authenticates a user and issues a JWT if credentials are valid.
     * 
     * @param req Login credentials
     * @return AuthResponse containing user details and JWT
     * @throws ResponseStatusException if authentication fails or account is inactive
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

    public AuthResponse refreshAccessToken(@NonNull String refreshToken) {
        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }

        String email = jwtService.extractSubject(refreshToken);
        Long userId = Long.valueOf(jwtService.extractClaim(refreshToken, "uid").toString());
        String jti = Objects.toString(jwtService.extractClaim(refreshToken, "jti"), "");
        String cacheKey = refreshTokenCacheKey(userId, jti);

        if (jti.isBlank() || !cacheService.hasKey(cacheKey)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token has expired or was revoked");
        }
        cacheService.delete(cacheKey);

        User u = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        if (u.getStatus() != User.UserStatus.ACTIVE || !u.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is locked or not activated");
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
     * @throws ResponseStatusException if user not found
     */
    public UserResponse getOne(@NonNull Long id) {
        return userRepository.findById(id)
                .map(userMapper::toDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    /**
     * Registers a new user with encrypted password and default roles.
     * 
     * @param req Registration details
     * @return Created user details
     * @throws ResponseStatusException if email or phone is already registered
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
     * Updates an existing user's profile and credentials.
     * 
     * @param id User ID
     * @param req Update details
     * @return Updated user details
     */
    @Transactional
    public UserResponse update(@NonNull Long id, @NonNull UserUpsertRequest req) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!u.getEmail().equals(req.getEmail()) && userRepository.existsByEmailIncludingDeleted(req.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

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
     * Updates the authenticated user's editable profile fields only.
     *
     * @param email Authenticated user's email
     * @param req   Profile update payload
     * @return Updated user profile
     */
    @Transactional
    public UserResponse updateCurrentProfile(@NonNull String email, @NonNull ProfileUpdateRequest req) {
        User u = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String nextPhone = StringUtils.hasText(req.getPhone()) ? req.getPhone().trim() : null;
        if (StringUtils.hasText(nextPhone) && !Objects.equals(u.getPhone(), nextPhone)
                && userRepository.existsByPhoneIncludingDeleted(nextPhone)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone number already exists");
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
        if (!userRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        userRepository.deleteById(id);
        log.info("User with id {} deleted", id);
        notificationService.notifyUserChange();
    }

    /**
     * Resets a user's password with a new provided value.
     * 
     * @param id User ID
     * @param newPassword Plain text new password
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
     * Changes the authenticated user's password after verifying the current password.
     *
     * @param email Authenticated user's email
     * @param req   Password change payload
     */
    @Transactional
    public void changeCurrentPassword(@NonNull String email, @NonNull PasswordChangeRequest req) {
        User u = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!passwordEncoder.matches(req.getCurrentPassword(), u.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }

        u.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(u);
        log.info("User changed own password: {}", u.getEmail());
    }

    /**
     * Uploads a new profile picture for the user and updates their avatar URL.
     * 
     * @param id User ID
     * @param file The image file
     * @return Updated user details
     */
    @Transactional
    public UserResponse uploadAvatar(@NonNull Long id, @NonNull MultipartFile file) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        try {
            String newUrl = imageManagerService.upload(file, "order_by_qr/avatars");
            if (StringUtils.hasText(u.getAvatarUrl())) {
                String oldUrl = u.getAvatarUrl();
                sideEffects.afterCommit(() -> imageManagerService.delete(oldUrl),
                        "delete replaced avatar image for user " + id);
            }
            sideEffects.afterRollback(() -> imageManagerService.delete(newUrl),
                    "delete rolled back avatar image for user " + id);
            u.setAvatarUrl(newUrl);
            userRepository.save(u);
            log.info("Avatar updated for user: {}", u.getEmail());
            return userMapper.toDto(u);
        } catch (IOException e) {
            log.error("Failed to upload avatar: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to upload avatar");
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
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
