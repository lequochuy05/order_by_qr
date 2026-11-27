package com.qros.shared.security;

import com.qros.shared.config.AppProperties;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class CookieRequestOriginValidator {

    private final AppProperties appProperties;

    public void requireAllowedOrigin(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (!StringUtils.hasText(origin)
                || !CorsOrigins.parse(appProperties.getCors().getAllowedOrigins())
                        .contains(origin)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Request origin is not allowed");
        }
    }
}
