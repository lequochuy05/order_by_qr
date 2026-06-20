package com.qros.shared.rate_limit;

import com.qros.core.config.AppProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class ClientAddressResolver {

    private final AppProperties appProperties;

    public String resolve(HttpServletRequest request) {
        if (appProperties.getSecurity().isTrustProxyHeaders()) {
            String forwardedFor = request.getHeader("X-Forwarded-For");
            if (StringUtils.hasText(forwardedFor) && !"unknown".equalsIgnoreCase(forwardedFor)) {
                return forwardedFor.split(",")[0].trim();
            }

            String realIp = request.getHeader("X-Real-IP");
            if (StringUtils.hasText(realIp) && !"unknown".equalsIgnoreCase(realIp)) {
                return realIp.trim();
            }
        }

        return request.getRemoteAddr();
    }
}
