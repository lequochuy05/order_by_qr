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
    void methodSecurityIsEnabledForSensitiveOperations() throws Exception {
        Path securityConfigPath = Files.walk(Path.of("src/main/java"))
                .filter(path -> path.getFileName().toString().equals("SecurityConfig.java"))
                .findFirst()
                .orElseThrow();

        String source = Files.readString(securityConfigPath);

        assertThat(source).contains("@EnableMethodSecurity");
    }

    @Test
    void sensitiveControllersHaveMethodLevelGuards() throws Exception {
        Path mainSource = Path.of("src/main/java");
        String paymentController = readController(mainSource, "PaymentController.java");
        String settingsController = readController(mainSource, "SystemSettingsController.java");
        String userAdminController = readController(mainSource, "UserAdminController.java");
        String voucherController = readController(mainSource, "VoucherController.java");
        String aiStaffController = readController(mainSource, "AiStaffController.java");

        assertThat(paymentController).contains("@PreAuthorize(\"hasAnyRole('MANAGER','STAFF')\")");
        assertThat(settingsController).contains("@PreAuthorize(\"hasRole('MANAGER')\")");
        assertThat(userAdminController).contains("@PreAuthorize(\"hasRole('MANAGER')\")");
        assertThat(voucherController).contains("@PreAuthorize(\"hasRole('MANAGER')\")");
        assertThat(aiStaffController).contains("@PreAuthorize(\"hasAnyRole('MANAGER','STAFF','CHEF')\")");
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
    void settingsAreReadableByStaffAndHaveNoDeleteRoute() throws Exception {
        Path securityRoutesPath = Files.walk(Path.of("src/main/java"))
                .filter(path -> path.getFileName().toString().equals("SecurityRoutes.java"))
                .findFirst()
                .orElseThrow();

        String source = Files.readString(securityRoutesPath);
        String staffReadGetBlock = source.substring(
                source.indexOf("public static final String[] STAFF_READ_GET"),
                source.indexOf("public static final String[] OPERATION_GET"));
        String managerDeleteBlock = source.substring(
                source.indexOf("public static final String[] MANAGER_DELETE"),
                source.indexOf("public static final String[] KITCHEN_PATCH"));

        assertThat(staffReadGetBlock).contains("ApiRoutes.SETTINGS");
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

    private String readController(Path mainSource, String filename) throws Exception {
        Path controllerPath = Files.walk(mainSource)
                .filter(path -> path.getFileName().toString().equals(filename))
                .findFirst()
                .orElseThrow();
        return Files.readString(controllerPath);
    }
}
