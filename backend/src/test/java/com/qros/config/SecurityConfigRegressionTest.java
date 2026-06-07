package com.qros.core.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class SecurityConfigRegressionTest {
    @Test
    void publicMatchersDoNotExposePaymentOrderOrTableWildcards() throws Exception {
        Path securityConfigPath = Files.walk(Path.of("src/main/java"))
                .filter(path -> path.getFileName().toString().equals("SecurityConfig.java"))
                .findFirst()
                .orElseThrow();

        String source = Files.readString(securityConfigPath);

        assertThat(source).doesNotContain(".requestMatchers(HttpMethod.POST, \"/api/orders/**\").permitAll()");
        assertThat(source).doesNotContain(".requestMatchers(HttpMethod.POST, \"/api/payments/**\").permitAll()");
        assertThat(source).doesNotContain(".requestMatchers(HttpMethod.GET, \"/api/payments/**\").permitAll()");
        assertThat(source).doesNotContain("\"/api/tables/**\",\n                \"/api/combos/**\"");
        assertThat(source)
                .contains(".requestMatchers(HttpMethod.POST, \"/api/orders\", \"/api/orders/preview\").permitAll()");
        assertThat(source).contains(".requestMatchers(\"/api/payments/**\").hasAnyRole(\"MANAGER\", \"STAFF\")");
        assertThat(source).contains(".requestMatchers(HttpMethod.GET, \"/api/public/**\")");
        assertThat(source).doesNotContain("\"/api/tables/code/*\"");
    }
}
