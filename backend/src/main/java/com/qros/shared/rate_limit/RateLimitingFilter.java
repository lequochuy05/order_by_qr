package com.qros.shared.rate_limit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * RateLimitingFilter - In-memory sliding-window rate limiter for public endpoints.
 */
@Component
@Order(1)
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS_PER_WINDOW = 30;
    private static final long WINDOW_DURATION_MS = 60_000L;
    private static final int MAX_LIMIT_KEYS = 100_000;

    private final Cache<String, Deque<Long>> requestLogs = Caffeine.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .maximumSize(MAX_LIMIT_KEYS)
            .build();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        boolean isAiChat = "/api/ai/chat".equals(path) && "POST".equalsIgnoreCase(method);
        boolean isPublicOrder = path.startsWith("/api/public/orders") && "POST".equalsIgnoreCase(method);
        boolean isStartSession =
                path.matches("^/api/public/tables/[^/]+/start-session$") && "POST".equalsIgnoreCase(method);
        boolean isHeartbeat = "/api/public/sessions/heartbeat".equals(path) && "POST".equalsIgnoreCase(method);
        boolean isRecommendation = path.startsWith("/api/public/recommendations") && "GET".equalsIgnoreCase(method);

        if (!isAiChat && !isPublicOrder && !isStartSession && !isHeartbeat && !isRecommendation) {
            filterChain.doFilter(request, response);
            return;
        }

        String limitKey = resolveLimitKey(request);

        long now = System.currentTimeMillis();

        Deque<Long> timestamps = requestLogs.get(limitKey, key -> new ArrayDeque<>());
        boolean allowed;

        synchronized (timestamps) {
            while (!timestamps.isEmpty() && now - timestamps.peekFirst() > WINDOW_DURATION_MS) {
                timestamps.removeFirst();
            }

            if (timestamps.size() >= MAX_REQUESTS_PER_WINDOW) {
                allowed = false;
            } else {
                timestamps.addLast(now);
                allowed = true;
            }
        }

        if (!allowed) {
            log.warn("[RateLimit] Key {} exceeded {} req/min on path {}", limitKey, MAX_REQUESTS_PER_WINDOW, path);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json; charset=UTF-8");
            response.getWriter().write("{\"status\":429,\"message\":\"Too many requests. Please try again later.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveLimitKey(HttpServletRequest request) {
        String sessionToken = request.getHeader("X-Session-Token");
        if (sessionToken != null && !sessionToken.isBlank()) {
            return "session:" + sessionToken.trim();
        }

        String tableToken = request.getHeader("X-Table-Token");
        if (tableToken != null && !tableToken.isBlank()) {
            return "table:" + tableToken.trim();
        }

        return "ip:" + resolveClientIp(request);
    }

    /**
     * Extracts the client IP, respecting common proxy headers.
     */
    private String resolveClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.trim();
        }
        return request.getRemoteAddr();
    }
}
