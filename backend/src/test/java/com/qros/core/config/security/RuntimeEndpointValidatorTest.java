package com.qros.core.config.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class RuntimeEndpointValidatorTest {

    @Test
    void allowsHttpsEndpoints() {
        assertThatCode(() -> RuntimeEndpointValidator.requireSecure("endpoint", "https://app.example", false))
                .doesNotThrowAnyException();
    }

    @Test
    void allowsLocalHttpOnlyInDevelopmentOrTests() {
        assertThatCode(() -> RuntimeEndpointValidator.requireSecure("endpoint", "http://localhost:5173", true))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> RuntimeEndpointValidator.requireSecure("endpoint", "http://localhost:5173", false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must use HTTPS");
    }

    @Test
    void rejectsRemoteHttpEndpoints() {
        assertThatThrownBy(() -> RuntimeEndpointValidator.requireSecure("endpoint", "http://app.example", true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must use HTTPS");
    }

    @Test
    void allowsCapacitorLocalOriginOnlyForCorsOrigins() {
        assertThatCode(() -> RuntimeEndpointValidator.requireSecureOrigin(
                        "app.cors.allowed-origins", "capacitor://localhost", false))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> RuntimeEndpointValidator.requireSecureOrigin(
                        "app.cors.allowed-origins", "capacitor://evil.example", false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must use HTTPS");
        assertThatThrownBy(() -> RuntimeEndpointValidator.requireSecure("app.base-url", "capacitor://localhost", false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must use HTTPS");
    }
}
