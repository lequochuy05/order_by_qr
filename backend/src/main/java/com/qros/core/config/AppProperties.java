package com.qros.core.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String baseUrl;
    private Frontend frontend = new Frontend();
    private Cors cors = new Cors();
    private Security security = new Security();
    private Cache cache = new Cache();
    private Email email = new Email();

    @Getter
    @Setter
    public static class Frontend {
        private String baseUrl;
    }

    @Getter
    @Setter
    public static class Cors {
        private String allowedOrigins = "";
    }

    @Getter
    @Setter
    public static class Security {
        private boolean enableDefaultAdmin;
        private String defaultAdminEmail;
        private String defaultAdminPassword;
        private boolean trustProxyHeaders;
    }

    @Getter
    @Setter
    public static class Cache {
        private String prefix = "qros:v3:";
    }

    @Getter
    @Setter
    public static class Email {
        private String fromName = "QROS";
        private String brandName = "QROS";
        private String copyright = "2026 QROS";
    }
}
