package com.qros.shared.rate_limit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qros.shared.constants.ApiRoutes;
import com.qros.shared.exception.AppException;
import com.qros.shared.response.ApiResponse;
import com.qros.shared.response.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * RateLimitingFilter - In-memory sliding-window rate limiter for public endpoints.
 */
@Component
@Order(1)
@Slf4j
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RedisRateLimiter rateLimiter;
    private final ClientAddressResolver clientAddressResolver;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        RateLimitPolicy policy = resolvePolicy(path, method);
        if (policy == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            rateLimiter.requireAllowed(
                    policy.scope(), clientAddressResolver.resolve(request), policy.maxRequests(), policy.window());
        } catch (AppException exception) {
            writeRateLimitResponse(response, exception);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private RateLimitPolicy resolvePolicy(String path, String method) {
        if ((ApiRoutes.PUBLIC + "/ai/chat").equals(path) && "POST".equalsIgnoreCase(method)) {
            return new RateLimitPolicy("public:ai", 10, Duration.ofMinutes(1));
        }

        if (path.startsWith(ApiRoutes.PUBLIC + "/orders") && "POST".equalsIgnoreCase(method)) {
            return new RateLimitPolicy("public:orders", 10, Duration.ofMinutes(1));
        }

        if (path.matches("^" + Pattern.quote(ApiRoutes.PUBLIC) + "/tables/[^/]+/start-session$")
                && "POST".equalsIgnoreCase(method)) {
            return new RateLimitPolicy("public:start-session", 10, Duration.ofMinutes(1));
        }

        if ((ApiRoutes.PUBLIC + "/sessions/heartbeat").equals(path) && "POST".equalsIgnoreCase(method)) {
            return new RateLimitPolicy("public:heartbeat", 60, Duration.ofMinutes(1));
        }

        if (path.startsWith(ApiRoutes.RECOMMENDATIONS) && "GET".equalsIgnoreCase(method)) {
            return new RateLimitPolicy("public:recommendations", 30, Duration.ofMinutes(1));
        }

        return null;
    }

    private void writeRateLimitResponse(HttpServletResponse response, AppException exception) throws IOException {
        int status = exception.getErrorCode().getStatus().value();
        Object retryAfter = exception.getDetails().get("retryAfterSeconds");
        if (retryAfter != null) {
            response.setHeader("Retry-After", retryAfter.toString());
        }

        ErrorResponse error = ErrorResponse.builder()
                .code(exception.getErrorCode().name())
                .message(exception.getMessage())
                .details(exception.getDetails())
                .build();
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(status, exception.getMessage(), error));
    }

    private record RateLimitPolicy(String scope, int maxRequests, Duration window) {}
}
