package com.qros.core.config.security;

import com.qros.core.config.AppProperties;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class CookieRequestOriginValidator {

    private static final List<String> DEFAULT_ALLOWED_ORIGINS =
            List.of("http://localhost:5173", "https://localhost", "https://wqros.vercel.app");

    private final AppProperties appProperties;

    public void requireAllowedOrigin(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (!StringUtils.hasText(origin) || !allowedOrigins().contains(origin)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Request origin is not allowed");
        }
    }

    private List<String> allowedOrigins() {
        String configuredOrigins = appProperties.getCors().getAllowedOrigins();
        if (!StringUtils.hasText(configuredOrigins)) {
            return DEFAULT_ALLOWED_ORIGINS;
        }
        return Arrays.stream(configuredOrigins.split("\\s*,\\s*"))
                .filter(StringUtils::hasText)
                .toList();
    }
}
