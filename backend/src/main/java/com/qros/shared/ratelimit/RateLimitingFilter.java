package com.qros.shared.ratelimit;

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
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import tools.jackson.databind.ObjectMapper;

/**
 * RateLimitingFilter - Redis-backed fixed-window rate limiter for public endpoints.
 * Policies are loaded from {@code rate-limit.rules} in application configuration.
 */
@Component
@Order(1)
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final PathPatternParser PATH_PATTERN_PARSER = PathPatternParser.defaultInstance;

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
        if (path == null) {
            return null;
        }
        PathContainer pathContainer = PathContainer.parsePath(path);
        for (CompiledRule rule : rules) {
            if (rule.methodPattern != null && !rule.methodPattern.equals(method)) {
                continue;
            }
            if (rule.pathPattern.matches(pathContainer)) {
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

    private static PathPattern compilePathPattern(String pattern) {
        String effectivePattern = (pattern == null || pattern.isBlank()) ? "/**" : pattern.trim();
        return PATH_PATTERN_PARSER.parse(effectivePattern);
    }

    private record CompiledRule(
            String scope, PathPattern pathPattern, String methodPattern, int maxRequests, Duration window) {}
}
