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

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Frontend getFrontend() {
        return frontend;
    }

    public void setFrontend(Frontend frontend) {
        this.frontend = frontend;
    }

    public Cors getCors() {
        return cors;
    }

    public void setCors(Cors cors) {
        this.cors = cors;
    }

    public Security getSecurity() {
        return security;
    }

    public void setSecurity(Security security) {
        this.security = security;
    }

    public Cache getCache() {
        return cache;
    }

    public void setCache(Cache cache) {
        this.cache = cache;
    }

    public Email getEmail() {
        return email;
    }

    public void setEmail(Email email) {
        this.email = email;
    }

    @Getter
    @Setter
    public static class Frontend {
        private String baseUrl;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }

    @Getter
    @Setter
    public static class Cors {
        private String allowedOrigins = "";

        public String getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(String allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }
    }

    @Getter
    @Setter
    public static class Security {
        private boolean enableDefaultAdmin;
        private String defaultAdminEmail;
        private String defaultAdminPassword;
        private boolean trustProxyHeaders;
        private String qrSecret;

        public boolean isEnableDefaultAdmin() {
            return enableDefaultAdmin;
        }

        public void setEnableDefaultAdmin(boolean enableDefaultAdmin) {
            this.enableDefaultAdmin = enableDefaultAdmin;
        }

        public String getDefaultAdminEmail() {
            return defaultAdminEmail;
        }

        public void setDefaultAdminEmail(String defaultAdminEmail) {
            this.defaultAdminEmail = defaultAdminEmail;
        }

        public String getDefaultAdminPassword() {
            return defaultAdminPassword;
        }

        public void setDefaultAdminPassword(String defaultAdminPassword) {
            this.defaultAdminPassword = defaultAdminPassword;
        }

        public boolean isTrustProxyHeaders() {
            return trustProxyHeaders;
        }

        public void setTrustProxyHeaders(boolean trustProxyHeaders) {
            this.trustProxyHeaders = trustProxyHeaders;
        }

        public String getQrSecret() {
            return qrSecret;
        }

        public void setQrSecret(String qrSecret) {
            this.qrSecret = qrSecret;
        }
    }

    @Getter
    @Setter
    public static class Cache {
        private String prefix = "qros:v3:";

        public String getPrefix() {
            return prefix;
        }

        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }
    }

    @Getter
    @Setter
    public static class Email {
        private String fromName = "QROS";
        private String brandName = "QROS";
        private String copyright = "2026 QROS";

        public String getFromName() {
            return fromName;
        }

        public void setFromName(String fromName) {
            this.fromName = fromName;
        }

        public String getBrandName() {
            return brandName;
        }

        public void setBrandName(String brandName) {
            this.brandName = brandName;
        }

        public String getCopyright() {
            return copyright;
        }

        public void setCopyright(String copyright) {
            this.copyright = copyright;
        }
    }
}
