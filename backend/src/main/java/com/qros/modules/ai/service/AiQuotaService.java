package com.qros.modules.ai.service;

import com.qros.modules.ai.config.AiQuotaProperties;
import com.qros.modules.ai.config.AiQuotaProperties.Policy;
import com.qros.shared.rate_limit.ClientAddressResolver;
import com.qros.shared.rate_limit.RedisRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiQuotaService {

    private final RedisRateLimiter rateLimiter;
    private final ClientAddressResolver clientAddressResolver;
    private final AiQuotaProperties properties;

    public void checkPublicChat(@NonNull HttpServletRequest request) {
        String clientAddress = clientAddressResolver.resolve(request);
        requireAllowed("ai:public:ip", clientAddress, properties.getPublicIp());
        requireAllowed("ai:public:ip:daily", clientAddress, properties.getPublicIpDaily());
    }

    public void checkStaffAssistant(@NonNull String username) {
        String subject = normalize(username);
        requireAllowed("ai:staff:user", subject, properties.getStaffUser());
        requireAllowed("ai:staff:user:daily", subject, properties.getStaffUserDaily());
    }

    public void checkMenuDescription(@NonNull String username) {
        String subject = normalize(username);
        requireAllowed("ai:menu-description:user", subject, properties.getMenuUser());
        requireAllowed("ai:menu-description:user:daily", subject, properties.getMenuUserDaily());
    }

    private void requireAllowed(String scope, String subject, Policy policy) {
        rateLimiter.requireAllowed(scope, subject, policy.getMaxRequests(), policy.getWindow(), false);
    }

    private String normalize(String username) {
        return username.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
