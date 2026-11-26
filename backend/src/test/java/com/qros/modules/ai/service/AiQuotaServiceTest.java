package com.qros.modules.ai.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qros.modules.ai.config.AiQuotaProperties;
import com.qros.shared.config.AppProperties;
import com.qros.shared.ratelimit.ClientAddressResolver;
import com.qros.shared.ratelimit.RedisRateLimiter;
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

    @Test
    void executiveInsightsQuotaChecksMinuteAndDailyUserQuota() {
        RedisRateLimiter rateLimiter = mock(RedisRateLimiter.class);
        AiQuotaService service = new AiQuotaService(
                rateLimiter, new ClientAddressResolver(new AppProperties()), new AiQuotaProperties());

        service.checkExecutiveInsights(" Manager@Example.COM ");

        verify(rateLimiter).requireAllowed("ai:insights:user", "manager@example.com", 10, Duration.ofMinutes(1), false);
        verify(rateLimiter)
                .requireAllowed("ai:insights:user:daily", "manager@example.com", 80, Duration.ofDays(1), false);
    }

    @Test
    void operationsAiQuotaChecksMinuteAndDailyUserQuota() {
        RedisRateLimiter rateLimiter = mock(RedisRateLimiter.class);
        AiQuotaService service = new AiQuotaService(
                rateLimiter, new ClientAddressResolver(new AppProperties()), new AiQuotaProperties());

        service.checkOperationsAi(" Chef@Example.COM ");

        verify(rateLimiter).requireAllowed("ai:operations:user", "chef@example.com", 15, Duration.ofMinutes(1), false);
        verify(rateLimiter)
                .requireAllowed("ai:operations:user:daily", "chef@example.com", 150, Duration.ofDays(1), false);
    }
}
