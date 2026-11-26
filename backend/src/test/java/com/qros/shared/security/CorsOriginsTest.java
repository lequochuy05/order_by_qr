package com.qros.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CorsOriginsTest {

    @Test
    void parsesConfiguredOriginsAndTrimsWhitespace() {
        assertThat(CorsOrigins.parse(" https://app.example , http://localhost:5173 "))
                .containsExactly("https://app.example", "http://localhost:5173");
    }

    @Test
    void returnsNoOriginsWhenConfigurationIsBlank() {
        assertThat(CorsOrigins.parse("  ")).isEmpty();
    }
}
