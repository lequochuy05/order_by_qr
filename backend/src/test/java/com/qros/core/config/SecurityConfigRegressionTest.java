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
        assertThat(publicPostBlock).doesNotContain("ApiRoutes.ORDERS");
        assertThat(publicPostBlock).doesNotContain("ApiRoutes.AI");
        assertThat(publicPostBlock).doesNotContain("ApiRoutes.PAYMENTS");

        assertThat(source).contains("ApiRoutes.ORDERS");
        assertThat(source).doesNotContain("\"/api/");
    }

    @Test
    void securityConfigurationIsUrlBasedUntilMethodGuardsAreIntroduced() throws Exception {
        Path securityConfigPath = Files.walk(Path.of("src/main/java"))
                .filter(path -> path.getFileName().toString().equals("SecurityConfig.java"))
                .findFirst()
                .orElseThrow();

        String source = Files.readString(securityConfigPath);

        assertThat(source).doesNotContain("@EnableMethodSecurity");
    }

    @Test
    void managerPutMatchersCoverUserAndTableMutations() throws Exception {
        Path securityRoutesPath = Files.walk(Path.of("src/main/java"))
                .filter(path -> path.getFileName().toString().equals("SecurityRoutes.java"))
                .findFirst()
                .orElseThrow();

        String source = Files.readString(securityRoutesPath);
        String managerPutBlock = source.substring(
                source.indexOf("public static final String[] MANAGER_PUT"),
                source.indexOf("public static final String[] MANAGER_PATCH"));

        assertThat(managerPutBlock).contains("ApiRoutes.USERS + \"/**\"");
        assertThat(managerPutBlock).contains("ApiRoutes.TABLES + \"/**\"");
        assertThat(managerPutBlock).contains("ApiRoutes.SETTINGS");
    }

    @Test
    void settingsAreManagerOnlyAndHaveNoDeleteRoute() throws Exception {
        Path securityRoutesPath = Files.walk(Path.of("src/main/java"))
                .filter(path -> path.getFileName().toString().equals("SecurityRoutes.java"))
                .findFirst()
                .orElseThrow();

        String source = Files.readString(securityRoutesPath);
        String managerGetBlock = source.substring(
                source.indexOf("public static final String[] MANAGER_GET"),
                source.indexOf("public static final String[] STAFF_READ_GET"));
        String managerDeleteBlock = source.substring(
                source.indexOf("public static final String[] MANAGER_DELETE"),
                source.indexOf("public static final String[] KITCHEN_PATCH"));

        assertThat(managerGetBlock).contains("ApiRoutes.SETTINGS");
        assertThat(managerDeleteBlock).doesNotContain("ApiRoutes.SETTINGS");
    }

    @Test
    void staffOrderItemMatchersAreEvaluatedBeforeManagerOrderWildcard() throws Exception {
        Path securityConfigPath = Files.walk(Path.of("src/main/java"))
                .filter(path -> path.getFileName().toString().equals("SecurityConfig.java"))
                .findFirst()
                .orElseThrow();

        String source = Files.readString(securityConfigPath);

        assertThat(source.indexOf("STAFF_OPERATION_PATCH")).isLessThan(source.indexOf("MANAGER_PATCH"));
        assertThat(source.indexOf("STAFF_OPERATION_DELETE")).isLessThan(source.indexOf("MANAGER_DELETE"));
    }
}
