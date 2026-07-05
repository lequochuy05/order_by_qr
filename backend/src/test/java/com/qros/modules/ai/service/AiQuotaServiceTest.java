package com.qros.modules.ai.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qros.core.config.AppProperties;
import com.qros.modules.ai.config.AiQuotaProperties;
import com.qros.shared.rate_limit.ClientAddressResolver;
import com.qros.shared.rate_limit.RedisRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class AiQuotaServiceTest {

    @Test
    void publicChatChecksMinuteAndDailyIpQuota() {
        RedisRateLimiter rateLimiter = mock(RedisRateLimiter.class);
        ClientAddressResolver clientAddressResolver = mock(ClientAddressResolver.class);
        AiQuotaService service = new AiQuotaService(rateLimiter, clientAddressResolver, new AiQuotaProperties());
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(clientAddressResolver.resolve(request)).thenReturn("203.0.113.10");

        service.checkPublicChat(request);

        verify(rateLimiter).requireAllowed("ai:public:ip", "203.0.113.10", 10, Duration.ofMinutes(1), false);
        verify(rateLimiter).requireAllowed("ai:public:ip:daily", "203.0.113.10", 100, Duration.ofDays(1), false);
    }

    @Test
    void staffAssistantQuotaUsesNormalizedUsername() {
        RedisRateLimiter rateLimiter = mock(RedisRateLimiter.class);
        AiQuotaService service = new AiQuotaService(
                rateLimiter, new ClientAddressResolver(new AppProperties()), new AiQuotaProperties());

        service.checkStaffAssistant(" Chef@Example.COM ");

        verify(rateLimiter).requireAllowed("ai:staff:user", "chef@example.com", 20, Duration.ofMinutes(1), false);
        verify(rateLimiter).requireAllowed("ai:staff:user:daily", "chef@example.com", 300, Duration.ofDays(1), false);
    }
}
