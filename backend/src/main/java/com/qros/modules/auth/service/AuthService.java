package com.qros.modules.auth.service;

import com.qros.modules.auth.dto.internal.AuthenticatedUser;
import com.qros.modules.auth.dto.internal.LoginResult;
import com.qros.modules.auth.dto.request.LoginRequest;
import com.qros.modules.auth.dto.response.LoginResponse;
import com.qros.modules.user.model.User;
import com.qros.modules.user.model.enums.UserStatus;
import com.qros.modules.user.repository.UserRepository;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.security.JwtService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * Authenticates a user with the provided login credentials.
     *
     * @param req The login request containing email and password
     * @return The result of the login attempt, including user info and access token
     */
    public LoginResult login(@NonNull LoginRequest req) {
        String email = req.email().trim().toLowerCase();

        User user = userRepository
                .findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE);
        }

        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

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
}
