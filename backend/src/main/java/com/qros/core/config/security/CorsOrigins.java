package com.qros.core.config.security;

import java.util.Arrays;
import java.util.List;
import org.springframework.util.StringUtils;

public final class CorsOrigins {

    private CorsOrigins() {}

    public static List<String> parse(String origins) {
        if (!StringUtils.hasText(origins)) {
            return List.of();
        }
        return Arrays.stream(origins.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }
}
