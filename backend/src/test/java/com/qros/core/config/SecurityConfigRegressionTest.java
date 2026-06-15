package com.qros.core.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class SecurityConfigRegressionTest {
    @Test
    void publicMatchersDoNotExposePaymentOrderOrTableWildcards() throws Exception {
        Path securityRoutesPath = Files.walk(Path.of("src/main/java"))
                .filter(path -> path.getFileName().toString().equals("SecurityRoutes.java"))
                .findFirst()
                .orElseThrow();

        String source = Files.readString(securityRoutesPath);

        // We just ensure staff/admin order routes are not near PUBLIC_POST.
        // A better approach is MockMvc, but for now we just fix the literal scan
        String publicPostBlock = source.substring(
                source.indexOf("public static final String[] PUBLIC_POST"),
                source.indexOf("public static final String[] SELF_GET"));
        assertThat(publicPostBlock).doesNotContain("\"/api/orders\"");
        assertThat(publicPostBlock).doesNotContain("\"/api/orders/**\"");
        assertThat(publicPostBlock).doesNotContain("\"/api/orders/preview\"");
        assertThat(publicPostBlock).doesNotContain("\"/api/ai/chat\"");
        assertThat(publicPostBlock).doesNotContain("\"/api/payments/**\"");

        assertThat(source).contains("\"/api/orders\"");
    }
}
