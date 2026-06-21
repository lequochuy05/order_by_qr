package com.qros.modules.auth.service;

import static org.mockito.Mockito.verify;

import com.qros.modules.auth.config.AuthRateLimitProperties;
import com.qros.shared.rate_limit.RedisRateLimiter;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AuthRateLimitServiceTest {

    @Test
    void loginUsesConfiguredIpAndNormalizedAccountPolicies() {
        RedisRateLimiter rateLimiter = Mockito.mock(RedisRateLimiter.class);
        AuthRateLimitProperties properties = new AuthRateLimitProperties();
        AuthRateLimitService service = new AuthRateLimitService(rateLimiter, properties);

        service.checkLogin("203.0.113.10", " Manager@Example.COM ");

        verify(rateLimiter).requireAllowed("auth:login:ip", "203.0.113.10", 10, Duration.ofMinutes(1));
        verify(rateLimiter).requireAllowed("auth:login:account", "manager@example.com", 5, Duration.ofMinutes(1));
    }
}
