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
        private boolean trustProxyHeaders;
    }
}
