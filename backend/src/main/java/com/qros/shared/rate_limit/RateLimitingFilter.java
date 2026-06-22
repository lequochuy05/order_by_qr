package com.qros.shared.rate_limit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qros.shared.exception.AppException;
import com.qros.shared.response.ApiResponse;
import com.qros.shared.response.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * RateLimitingFilter - Redis-backed fixed-window rate limiter for public endpoints.
 * Policies are loaded from {@code rate-limit.rules} in application configuration.
 */
@Component
@Order(1)
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RedisRateLimiter rateLimiter;
    private final ClientAddressResolver clientAddressResolver;
    private final ObjectMapper objectMapper;
    private final List<CompiledRule> rules;

    public RateLimitingFilter(
            RedisRateLimiter rateLimiter,
            ClientAddressResolver clientAddressResolver,
            ObjectMapper objectMapper,
            RateLimitProperties properties) {
        this.rateLimiter = rateLimiter;
        this.clientAddressResolver = clientAddressResolver;
        this.objectMapper = objectMapper;
        this.rules = properties.getRules().stream()
                .map(rule -> new CompiledRule(
                        rule.getScope(),
                        compilePathPattern(rule.getPath()),
                        rule.getMethod() != null ? rule.getMethod().toUpperCase() : null,
                        rule.getMaxRequests(),
                        rule.getWindow()))
                .toList();
        if (!rules.isEmpty()) {
            log.info("Loaded {} rate-limit rule(s)", rules.size());
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        CompiledRule rule = resolveRule(path, method);
        if (rule == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            rateLimiter.requireAllowed(
                    rule.scope, clientAddressResolver.resolve(request), rule.maxRequests, rule.window);
        } catch (AppException exception) {
            writeRateLimitResponse(response, exception);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private CompiledRule resolveRule(String path, String method) {
        for (CompiledRule rule : rules) {
            if (rule.methodPattern != null && !rule.methodPattern.equals(method)) {
                continue;
            }
            if (rule.pathPattern.matcher(path).matches()) {
                return rule;
            }
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

    private static Pattern compilePathPattern(String pattern) {
        if (pattern == null) {
            return Pattern.compile(".*");
        }
        String regex = Pattern.quote(pattern).replace("\\*\\*", ".*").replace("\\*", "[^/]*");
        return Pattern.compile("^" + regex + "$");
    }

    private record CompiledRule(
            String scope, Pattern pathPattern, String methodPattern, int maxRequests, Duration window) {}
}
