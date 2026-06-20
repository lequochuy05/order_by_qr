package com.qros.modules.payment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "payos")
public class PayOSProperties {

    private String clientId;
    private String apiKey;
    private String checksumKey;
}
