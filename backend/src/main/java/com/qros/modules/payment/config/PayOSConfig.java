package com.qros.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import vn.payos.PayOS;
import vn.payos.core.ClientOptions;

/**
 * PayOSConfig - Configure PayOS client with Client ID, API Key, and Checksum
 * Key.
 */
@Configuration
public class PayOSConfig {

    /**
     * Client ID from PayOS
     */
    @Value("${payos.client-id}")
    private String clientId;

    /**
     * API Key from PayOS
     */
    @Value("${payos.api-key}")
    private String apiKey;

    /**
     * Checksum Key from PayOS
     */
    @Value("${payos.checksum-key}")
    private String checksumKey;

    /**
     * Create PayOS client instance.
     * 
     * @return PayOS client instance
     */
    @Bean
    public PayOS payOS() {
        ClientOptions options = ClientOptions.builder()
                .clientId(clientId)
                .apiKey(apiKey)
                .checksumKey(checksumKey)
                .logLevel(ClientOptions.LogLevel.INFO)
                .build();

        return new PayOS(options);
    }
}
