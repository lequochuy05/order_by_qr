package com.sacmauquan.qrordering.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class SecurityConfigRegressionTest {
    @Test
    void publicMatchersDoNotExposePaymentOrderOrTableWildcards() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/sacmauquan/qrordering/config/SecurityConfig.java"));

        assertThat(source).doesNotContain(".requestMatchers(HttpMethod.POST, \"/api/orders/**\").permitAll()");
        assertThat(source).doesNotContain(".requestMatchers(HttpMethod.POST, \"/api/payments/**\").permitAll()");
        assertThat(source).doesNotContain(".requestMatchers(HttpMethod.GET, \"/api/payments/**\").permitAll()");
        assertThat(source).doesNotContain("\"/api/tables/**\",\n                \"/api/combos/**\"");
        assertThat(source).contains(".requestMatchers(HttpMethod.POST, \"/api/orders\", \"/api/orders/preview\").permitAll()");
        assertThat(source).contains(".requestMatchers(\"/api/payments/**\").hasAnyRole(\"MANAGER\", \"STAFF\")");
        assertThat(source).contains("\"/api/tables/code/*\"");
    }
}
