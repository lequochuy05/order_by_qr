package com.qros.core.config.security;

import com.qros.core.config.AppProperties;
import java.net.URI;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class RuntimeEndpointValidator implements org.springframework.beans.factory.InitializingBean {

    private final AppProperties appProperties;
    private final Environment environment;

    @Override
    public void afterPropertiesSet() {
        boolean allowLocalHttp = environment.acceptsProfiles(Profiles.of("dev", "test"));
        requireSecure("app.base-url", appProperties.getBaseUrl(), allowLocalHttp);
        requireSecure("app.frontend.base-url", appProperties.getFrontend().getBaseUrl(), allowLocalHttp);
        for (String origin : CorsOrigins.parse(appProperties.getCors().getAllowedOrigins())) {
            requireSecureOrigin("app.cors.allowed-origins", origin, allowLocalHttp);
        }
    }

    static void requireSecure(String propertyName, String endpoint, boolean allowLocalHttp) {
        validateEndpoint(propertyName, endpoint, allowLocalHttp, false);
    }

    static void requireSecureOrigin(String propertyName, String origin, boolean allowLocalHttp) {
        validateEndpoint(propertyName, origin, allowLocalHttp, true);
    }

    private static void validateEndpoint(
            String propertyName, String endpoint, boolean allowLocalHttp, boolean allowCapacitorLocalOrigin) {
        if (!StringUtils.hasText(endpoint)) {
            throw new IllegalStateException(propertyName + " must be configured");
        }

        URI uri;
        try {
            uri = URI.create(endpoint);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(propertyName + " must be a valid absolute URL", exception);
        }

        if (!uri.isAbsolute() || uri.getHost() == null) {
            throw new IllegalStateException(propertyName + " must be a valid absolute URL");
        }

        String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
        if ("https".equals(scheme)) {
            return;
        }

        String host = uri.getHost().toLowerCase(Locale.ROOT);
        boolean capacitorLocalOrigin = allowCapacitorLocalOrigin
                && "capacitor".equals(scheme)
                && "localhost".equals(host)
                && uri.getPort() == -1
                && !StringUtils.hasText(uri.getRawPath())
                && uri.getRawQuery() == null
                && uri.getRawFragment() == null;
        if (capacitorLocalOrigin) {
            return;
        }

        boolean localHttp = "http".equals(scheme)
                && allowLocalHttp
                && (host.equals("localhost") || host.equals("127.0.0.1") || host.equals("::1") || host.equals("[::1]"));
        if (!localHttp) {
            throw new IllegalStateException(propertyName + " must use HTTPS outside local development");
        }
    }
}
