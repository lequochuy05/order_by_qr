package com.qros.shared.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.qros.shared.config.AppProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

class ClientAddressResolverTest {

    @Test
    void usesRemoteAddressWhenProxyHeadersAreNotTrusted() {
        AppProperties properties = new AppProperties();
        properties.getSecurity().setTrustProxyHeaders(false);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.10");
        when(request.getRemoteAddr()).thenReturn("10.0.0.5");

        assertThat(new ClientAddressResolver(properties).resolve(request)).isEqualTo("10.0.0.5");
    }

    @Test
    void usesFirstKnownForwardedAddressWhenProxyHeadersAreTrusted() {
        AppProperties properties = new AppProperties();
        properties.getSecurity().setTrustProxyHeaders(true);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("unknown, 203.0.113.10, 10.0.0.5");

        assertThat(new ClientAddressResolver(properties).resolve(request)).isEqualTo("203.0.113.10");
    }

    @Test
    void fallsBackToRealIpWhenForwardedForIsUnknown() {
        AppProperties properties = new AppProperties();
        properties.getSecurity().setTrustProxyHeaders(true);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("unknown");
        when(request.getHeader("X-Real-IP")).thenReturn("198.51.100.22");

        assertThat(new ClientAddressResolver(properties).resolve(request)).isEqualTo("198.51.100.22");
    }

    @Test
    void ignoresSpoofedLeftmostForwardedIpAndUsesRightmostPublicClientIp() {
        AppProperties properties = new AppProperties();
        properties.getSecurity().setTrustProxyHeaders(true);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("198.51.100.99, 203.0.113.10, 10.0.0.5");

        assertThat(new ClientAddressResolver(properties).resolve(request)).isEqualTo("203.0.113.10");
    }

    @Test
    void rejectsNonIpGarbageInForwardedHeadersAndFallsBackToRemoteAddr() {
        AppProperties properties = new AppProperties();
        properties.getSecurity().setTrustProxyHeaders(true);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("spoofed-bypass-key-123");
        when(request.getHeader("X-Real-IP")).thenReturn("not-an-ip");
        when(request.getRemoteAddr()).thenReturn("203.0.113.50");

        assertThat(new ClientAddressResolver(properties).resolve(request)).isEqualTo("203.0.113.50");
    }
}
