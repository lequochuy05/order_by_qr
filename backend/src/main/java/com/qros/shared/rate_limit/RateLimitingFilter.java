package com.qros.shared.rate_limit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RateLimitingFilter - In-memory sliding-window rate limiter for the public AI chat endpoint.
 * <p>
 * Uses a ConcurrentHashMap of per-IP timestamp deques. Requests exceeding the limit
 * (default: 30 requests per 60-second window per IP) receive HTTP 429.
 * Old entries are opportunistically pruned on each request.
 * <p>
 * No external dependencies (Redis, Bucket4j, Resilience4j) — lightweight self-contained filter.
 */
@Component
@Order(1)
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS_PER_WINDOW = 30;
    private static final long WINDOW_DURATION_MS = 60_000L;

    private final ConcurrentHashMap<String, Deque<Long>> requestLogs = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        if (!"/api/ai/chat".equals(path) || !"POST".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = resolveClientIp(request);
        long now = System.currentTimeMillis();

        Deque<Long> timestamps = requestLogs.computeIfAbsent(clientIp, key -> new ArrayDeque<>());
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
            log.warn("[RateLimit] IP {} exceeded {} req/min on /api/ai/chat", clientIp, MAX_REQUESTS_PER_WINDOW);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json; charset=UTF-8");
            response.getWriter().write("{\"status\":429,\"message\":\"Too many requests. Please try again later.\"}");
            return;
        }

        filterChain.doFilter(request, response);
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
