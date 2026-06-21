package com.qros.shared.security;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {

    private String secret;
    private long expirationMs;
    private long refreshExpirationMs;
    private String refreshCookieName;
    private boolean refreshCookieSecure;

    @Pattern(regexp = "Lax|Strict|None", message = "Refresh cookie SameSite must be Lax, Strict, or None")
    private String refreshCookieSameSite = "Lax";

    @AssertTrue(message = "Refresh cookie must be secure when SameSite=None")
    public boolean isRefreshCookieConfigurationSecure() {
        return !"None".equals(refreshCookieSameSite) || refreshCookieSecure;
    }
}
