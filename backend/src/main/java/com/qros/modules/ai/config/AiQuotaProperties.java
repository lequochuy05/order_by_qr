package com.qros.modules.ai.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "ai.quota")
public class AiQuotaProperties {

    @Valid
    private Policy publicIp = new Policy(10, Duration.ofMinutes(1));

    @Valid
    private Policy publicIpDaily = new Policy(100, Duration.ofDays(1));

    @Valid
    private Policy staffUser = new Policy(20, Duration.ofMinutes(1));

    @Valid
    private Policy staffUserDaily = new Policy(300, Duration.ofDays(1));

    @Valid
    private Policy menuUser = new Policy(10, Duration.ofMinutes(1));

    @Valid
    private Policy menuUserDaily = new Policy(100, Duration.ofDays(1));

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Policy {
        @Min(1)
        private int maxRequests;

        @NotNull private Duration window;

        private Policy(int maxRequests, Duration window) {
            this.maxRequests = maxRequests;
            this.window = window;
        }
    }
}
