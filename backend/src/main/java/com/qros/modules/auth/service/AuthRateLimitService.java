package com.qros.modules.auth.service;

import com.qros.modules.auth.config.AuthRateLimitProperties;
import com.qros.modules.auth.config.AuthRateLimitProperties.Policy;
import com.qros.shared.rate_limit.RedisRateLimiter;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthRateLimitService {

    private final RedisRateLimiter rateLimiter;
    private final AuthRateLimitProperties properties;

    public void checkLogin(@NonNull String clientAddress, @NonNull String email) {
        requireAllowed("auth:login:ip", clientAddress, properties.getLoginIp());
        requireAllowed("auth:login:account", normalize(email), properties.getLoginAccount());
    }

    public void checkForgotPassword(@NonNull String clientAddress, @NonNull String accountKey) {
        requireAllowed("auth:forgot:ip", clientAddress, properties.getForgotPasswordIp());
        requireAllowed("auth:forgot:account", normalize(accountKey), properties.getForgotPasswordAccount());
    }

    public void checkPasswordReset(@NonNull String clientAddress, @NonNull String accountKey) {
        requireAllowed("auth:reset:ip", clientAddress, properties.getPasswordResetIp());
        requireAllowed("auth:reset:account", normalize(accountKey), properties.getPasswordResetAccount());
    }

    public void checkRefresh(@NonNull String clientAddress) {
        requireAllowed("auth:refresh:ip", clientAddress, properties.getRefreshIp());
    }

    private void requireAllowed(String scope, String subject, Policy policy) {
        rateLimiter.requireAllowed(scope, subject, policy.getMaxRequests(), policy.getWindow());
    }

    private String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
