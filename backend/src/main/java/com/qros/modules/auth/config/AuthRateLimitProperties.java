package com.qros.modules.auth.config;

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
@ConfigurationProperties(prefix = "auth.rate-limit")
public class AuthRateLimitProperties {

    @Valid
    private Policy loginIp = new Policy(10, Duration.ofMinutes(1));

    @Valid
    private Policy loginAccount = new Policy(5, Duration.ofMinutes(1));

    @Valid
    private Policy forgotPasswordIp = new Policy(10, Duration.ofMinutes(15));

    @Valid
    private Policy forgotPasswordAccount = new Policy(3, Duration.ofMinutes(15));

    @Valid
    private Policy passwordResetIp = new Policy(10, Duration.ofMinutes(15));

    @Valid
    private Policy passwordResetAccount = new Policy(5, Duration.ofMinutes(15));

    @Valid
    private Policy refreshIp = new Policy(30, Duration.ofMinutes(1));

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
