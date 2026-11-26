package com.qros.modules.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

@ConfigurationProperties(prefix = "gemini")
public record GeminiProperties(
        boolean enabled,
        String apiKey,
        String apiUrl,
        String fallbackApiUrl,
        int connectTimeoutSeconds,
        int readTimeoutSeconds,
        double temperature,
        double topP,
        int maxOutputTokens) {

    @ConstructorBinding
    public GeminiProperties {
        if (connectTimeoutSeconds <= 0) {
            connectTimeoutSeconds = 10;
        }
        if (readTimeoutSeconds <= 0) {
            readTimeoutSeconds = 25;
        }
    }

    public GeminiProperties(
            boolean enabled,
            String apiKey,
            String apiUrl,
            int connectTimeoutSeconds,
            int readTimeoutSeconds,
            double temperature,
            double topP,
            int maxOutputTokens) {
        this(
                enabled,
                apiKey,
                apiUrl,
                null,
                connectTimeoutSeconds,
                readTimeoutSeconds,
                temperature,
                topP,
                maxOutputTokens);
    }
}
