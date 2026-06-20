package com.qros.modules.payment.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.payos.PayOS;
import vn.payos.core.ClientOptions;

/**
 * PayOSConfig - Configure PayOS client with Client ID, API Key, and Checksum
 * Key.
 */
@Configuration
@RequiredArgsConstructor
public class PayOSConfig {

    private final PayOSProperties properties;

    /**
     * Create PayOS client instance.
     *
     * @return PayOS client instance
     */
    @Bean
    public PayOS payOS() {
        ClientOptions options = ClientOptions.builder()
                .clientId(properties.getClientId())
                .apiKey(properties.getApiKey())
                .checksumKey(properties.getChecksumKey())
                .logLevel(ClientOptions.LogLevel.INFO)
                .build();

        return new PayOS(options);
    }
}
