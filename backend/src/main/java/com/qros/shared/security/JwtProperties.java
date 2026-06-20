package com.qros.shared.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {

    private String secret;
    private long expirationMs;
    private long refreshExpirationMs;
    private String refreshCookieName;
    private boolean refreshCookieSecure;
}
