package com.qros.modules.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gemini")
public record GeminiProperties(
        boolean enabled,
        String apiKey,
        String apiUrl,
        int connectTimeoutSeconds,
        int readTimeoutSeconds,
        double temperature,
        double topP,
        int maxOutputTokens) {}
