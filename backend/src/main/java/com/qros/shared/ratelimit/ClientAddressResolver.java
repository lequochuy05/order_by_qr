package com.qros.shared.ratelimit;

import com.qros.shared.config.AppProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class ClientAddressResolver {

    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^(?:(?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)\\.){3}(?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)$");
    private static final Pattern IPV6_LITERAL_PATTERN = Pattern.compile("^[0-9a-fA-F:]+(?:%[0-9a-zA-Z._-]+)?$");

    private final AppProperties appProperties;

    public String resolve(HttpServletRequest request) {
        String remoteAddr = normalizeValidIp(request.getRemoteAddr());
        if (appProperties.getSecurity().isTrustProxyHeaders()
                && (remoteAddr == null || isInternalProxyAddress(remoteAddr))) {
            String cfConnectingIp = normalizeValidIp(request.getHeader("CF-Connecting-IP"));
            if (cfConnectingIp != null) {
                return cfConnectingIp;
            }

            String forwardedFor = request.getHeader("X-Forwarded-For");
            String forwardedAddress = resolveTrustedForwardedAddress(forwardedFor);
            if (forwardedAddress != null) {
                return forwardedAddress;
            }

            String realIp = normalizeValidIp(request.getHeader("X-Real-IP"));
            if (realIp != null) {
                return realIp;
            }
        }

        return request.getRemoteAddr();
    }

    /**
     * Traverses X-Forwarded-For from right to left so client-supplied spoofed prefixes
     * on the left are ignored when reverse proxies append the connecting client IP.
     * Skips internal/private proxy hops and returns the rightmost public IP (or the
     * rightmost valid private IP if all hops are private).
     */
    private String resolveTrustedForwardedAddress(String forwardedFor) {
        if (!StringUtils.hasText(forwardedFor)) {
            return null;
        }
        String[] parts = forwardedFor.split(",");
        String fallbackPrivateIp = null;

        for (int i = parts.length - 1; i >= 0; i--) {
            String ip = normalizeValidIp(parts[i]);
            if (ip == null) {
                continue;
            }
            if (!isInternalProxyAddress(ip)) {
                return ip;
            }
            if (fallbackPrivateIp == null) {
                fallbackPrivateIp = ip;
            }
        }
        return fallbackPrivateIp;
    }

    private String normalizeValidIp(String rawCandidate) {
        if (!StringUtils.hasText(rawCandidate)) {
            return null;
        }
        String candidate = rawCandidate.trim();
        if ("unknown".equalsIgnoreCase(candidate)) {
            return null;
        }
        if (candidate.startsWith("[") && candidate.contains("]")) {
            candidate = candidate.substring(1, candidate.indexOf(']'));
        } else if (candidate.indexOf(':') > 0
                && candidate.indexOf(':') == candidate.lastIndexOf(':')
                && candidate.contains(".")) {
            candidate = candidate.substring(0, candidate.indexOf(':'));
        }
        if (!IPV4_PATTERN.matcher(candidate).matches()
                && !(candidate.contains(":")
                        && IPV6_LITERAL_PATTERN.matcher(candidate).matches())) {
            return null;
        }
        try {
            InetAddress.getByName(candidate);
            return candidate;
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isInternalProxyAddress(String ip) {
        try {
            InetAddress address = InetAddress.getByName(ip);
            if (address.isAnyLocalAddress()
                    || address.isLoopbackAddress()
                    || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress()) {
                return true;
            }
            byte[] bytes = address.getAddress();
            // Carrier-Grade NAT (RFC 6598: 100.64.0.0/10) used by cloud load balancers/proxies (e.g., Render)
            if (bytes.length == 4 && (bytes[0] & 0xFF) == 100 && (bytes[1] & 0xC0) == 64) {
                return true;
            }
            // IPv6 Unique Local Address (fc00::/7)
            return bytes.length == 16 && (bytes[0] & 0xFE) == 0xFC;
        } catch (Exception ignored) {
            return false;
        }
    }
}
