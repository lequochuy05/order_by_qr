package com.qros.core.config.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.qros.shared.config.AppProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
        classes = RuntimeEndpointValidatorIntegrationTest.TestApplication.class,
        properties = {
            "app.base-url=https://api.example.test",
            "app.frontend.base-url=https://app.example.test",
            "app.cors.allowed-origins=https://app.example.test"
        })
@ActiveProfiles("prod")
class RuntimeEndpointValidatorIntegrationTest {

    @Autowired
    private RuntimeEndpointValidator validator;

    @Autowired
    private AppProperties appProperties;

    @Autowired
    private ConfigurableEnvironment environment;

    @Test
    void acceptsHttpsAndRejectsHttpOriginsOutsideLocalProfiles() {
        assertThatCode(validator::afterPropertiesSet).doesNotThrowAnyException();

        appProperties.setBaseUrl("http://api.example.test");
        assertThatThrownBy(validator::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.base-url must use HTTPS");

        appProperties.setBaseUrl("https://api.example.test");
        appProperties.getFrontend().setBaseUrl("http://app.example.test");
        assertThatThrownBy(validator::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.frontend.base-url must use HTTPS");

        appProperties.getFrontend().setBaseUrl("https://app.example.test");
        appProperties.getCors().setAllowedOrigins("https://app.example.test,https://localhost,capacitor://localhost");
        assertThatCode(validator::afterPropertiesSet).doesNotThrowAnyException();

        appProperties.getCors().setAllowedOrigins("https://app.example.test,http://api.example.test");
        assertThatThrownBy(validator::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.cors.allowed-origins must use HTTPS");
    }

    @Test
    void allowsLocalhostHttpOnlyForDevAndTestProfiles() {
        appProperties.setBaseUrl("http://localhost:8080");
        appProperties.getFrontend().setBaseUrl("http://127.0.0.1:5173");
        appProperties.getCors().setAllowedOrigins("https://app.example.test,http://localhost:5173");

        environment.setActiveProfiles("dev");
        assertThatCode(validator::afterPropertiesSet).doesNotThrowAnyException();

        environment.setActiveProfiles("test");
        assertThatCode(validator::afterPropertiesSet).doesNotThrowAnyException();

        environment.setActiveProfiles("prod");
        assertThatThrownBy(validator::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.base-url must use HTTPS");
    }

    @Configuration
    @EnableConfigurationProperties(AppProperties.class)
    @Import(RuntimeEndpointValidator.class)
    static class TestApplication {}
}
