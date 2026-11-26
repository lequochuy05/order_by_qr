package com.qros.modules.table.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.qros.shared.config.AppProperties;
import com.qros.shared.exception.BusinessException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class TableQrSecurityServiceTest {

    @Test
    void requiresAnExplicitQrSigningSecret() {
        assertThatThrownBy(() -> new TableQrSecurityService(new AppProperties()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.security.qr-secret")
                .hasMessageContaining("QR_SIGNING_SECRET");
    }

    @Test
    void devProfileDoesNotProvideHardCodedQrSecretFallback() throws Exception {
        String devYaml = Files.readString(Path.of("src/main/resources/application-dev.yml"));

        assertThat(devYaml).contains("qr-secret: ${QR_SIGNING_SECRET:}").doesNotContain("app:\n  qr:");
        assertThat(Files.notExists(Path.of("src/main/resources/application-dev.properties")))
                .isTrue();
    }

    @Test
    void signsAndValidatesTableCodesWithTheConfiguredSecret() {
        AppProperties properties = new AppProperties();
        properties.getSecurity().setQrSecret("test-qr-signing-secret");
        TableQrSecurityService service = new TableQrSecurityService(properties);

        String signature = service.generateSignature("T-01");

        assertThat(signature).hasSize(64);
        assertThat(service.isSignatureValid("T-01", signature)).isTrue();
        assertThat(service.isSignatureValid("T-02", signature)).isFalse();
        assertThatThrownBy(() -> service.validateSignature("T-01", "invalid")).isInstanceOf(BusinessException.class);
    }

    @Test
    void supportsAppQrSecretConfigProperty() {
        AppProperties properties = new AppProperties();
        properties.getQr().setSecret("test-qr-signing-secret-via-qr-object");
        TableQrSecurityService service = new TableQrSecurityService(properties);

        String signature = service.generateSignature("T-01");
        assertThat(signature).hasSize(64);
        assertThat(service.isSignatureValid("T-01", signature)).isTrue();
    }
}
