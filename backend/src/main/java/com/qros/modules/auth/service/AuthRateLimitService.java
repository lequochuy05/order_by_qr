package com.qros.modules.auth.service;

import com.qros.shared.rate_limit.ClientAddressResolver;
import com.qros.shared.rate_limit.RedisRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthRateLimitService {

    private final RedisRateLimiter rateLimiter;
    private final ClientAddressResolver clientAddressResolver;

    public void checkLogin(@NonNull HttpServletRequest request, @NonNull String email) {
        rateLimiter.requireAllowed("auth:login:ip", clientAddressResolver.resolve(request), 10, Duration.ofMinutes(1));
        rateLimiter.requireAllowed("auth:login:account", normalize(email), 5, Duration.ofMinutes(1));
    }

    public void checkForgotPassword(@NonNull HttpServletRequest request, @NonNull String accountKey) {
        rateLimiter.requireAllowed(
                "auth:forgot:ip", clientAddressResolver.resolve(request), 10, Duration.ofMinutes(15));
        rateLimiter.requireAllowed("auth:forgot:account", normalize(accountKey), 3, Duration.ofMinutes(15));
    }

    public void checkPasswordReset(@NonNull HttpServletRequest request, @NonNull String accountKey) {
        rateLimiter.requireAllowed("auth:reset:ip", clientAddressResolver.resolve(request), 10, Duration.ofMinutes(15));
        rateLimiter.requireAllowed("auth:reset:account", normalize(accountKey), 5, Duration.ofMinutes(15));
    }

    public void checkRefresh(@NonNull HttpServletRequest request) {
        rateLimiter.requireAllowed(
                "auth:refresh:ip", clientAddressResolver.resolve(request), 30, Duration.ofMinutes(1));
    }

    private String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
