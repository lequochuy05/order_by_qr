package com.qros.modules.auth.service;

import com.qros.modules.auth.dto.internal.AuthenticatedUser;
import com.qros.modules.auth.dto.internal.LoginResult;
import com.qros.modules.auth.dto.request.LoginRequest;
import com.qros.modules.auth.dto.response.LoginResponse;
import com.qros.modules.user.model.User;
import com.qros.modules.user.model.enums.UserRole;
import com.qros.modules.user.model.enums.UserStatus;
import com.qros.modules.user.repository.UserRepository;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.security.JwtService;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private static final String DEFAULT_DUMMY_BCRYPT_HASH =
            "$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AccountLockoutService accountLockoutService;
    private final Admin2faService admin2faService;
    private final String dummyPasswordHash;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AccountLockoutService accountLockoutService) {
        this(userRepository, passwordEncoder, jwtService, accountLockoutService, null);
    }

    @Autowired
    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AccountLockoutService accountLockoutService,
            Admin2faService admin2faService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.accountLockoutService = accountLockoutService;
        this.admin2faService = admin2faService;
        String generatedHash = null;
        try {
            generatedHash = passwordEncoder.encode("qros-dummy-timing-defense-password");
        } catch (Exception ignored) {
            // fallback for mocked encoders in tests
        }
        this.dummyPasswordHash =
                (generatedHash != null && !generatedHash.isBlank()) ? generatedHash : DEFAULT_DUMMY_BCRYPT_HASH;
    }

    /**
     * Authenticates a user with the provided login credentials.
     * Defends against timing-based user enumeration attacks by ensuring
     * password hashing (BCrypt) runs for both existing and non-existing accounts.
     * Enforces account lockout after consecutive failed password attempts.
     * For ADMIN users, initiates Two-Factor Authentication (Email OTP) before issuing session tokens.
     *
     * @param req The login request containing email and password
     * @return The result of the login attempt, including user info and access token (or 2FA ticket)
     */
    public LoginResult login(@NonNull LoginRequest req) {
        String email = req.email().trim().toLowerCase();

        accountLockoutService.checkLocked(email);

        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);

        boolean passwordMatches;
        if (user != null) {
            passwordMatches = passwordEncoder.matches(req.password(), user.getPassword());
        } else {
            passwordEncoder.matches(req.password(), dummyPasswordHash);
            passwordMatches = false;
        }

        if (user == null || !passwordMatches) {
            accountLockoutService.recordFailedAttempt(email);
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE);
        }

        // Đối với tài khoản ADMIN, bắt buộc xác thực 2 bước (2FA OTP qua Email)
        if (user.getRole() == UserRole.ADMIN && admin2faService != null) {
            String preAuthToken = admin2faService.initiate2fa(user);
            LoginResponse response = LoginResponse.require2fa(maskEmail(user.getEmail()), preAuthToken);
            return new LoginResult(response, null);
        }

        accountLockoutService.recordSuccessfulLogin(email);

        String accessToken = createAccessToken(user);
        LoginResponse response = LoginResponse.of(user, accessToken);
        AuthenticatedUser authUser = AuthenticatedUser.from(user);

        return new LoginResult(response, authUser);
    }

    /**
     * Completes login after successful 2FA OTP verification for ADMIN account.
     */
    public LoginResult complete2faLogin(@NonNull String email) {
        User user = userRepository
                .findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE);
        }

        accountLockoutService.recordSuccessfulLogin(email);

        String accessToken = createAccessToken(user);
        LoginResponse response = LoginResponse.of(user, accessToken);
        AuthenticatedUser authUser = AuthenticatedUser.from(user);

        return new LoginResult(response, authUser);
    }

    /**
     * Creates a JWT access token for the authenticated user.
     *
     * @param user The authenticated user
     * @return The generated access token
     */
    public String createAccessToken(User user) {
        return jwtService.generateAccessToken(
                user.getEmail(),
                Map.of(
                        "uid", user.getId(),
                        "role", user.getRole().name()));
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        int atIndex = email.indexOf('@');
        String name = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        if (name.length() <= 2) {
            return name.charAt(0) + "***" + domain;
        }
        return name.charAt(0) + "***" + name.charAt(name.length() - 1) + domain;
    }
}
