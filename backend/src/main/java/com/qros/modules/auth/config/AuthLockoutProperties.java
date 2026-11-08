package com.qros.modules.auth.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "auth.lockout")
public class AuthLockoutProperties {

    @Min(1)
    private int maxFailedAttempts = 5;

    @NotNull private Duration duration = Duration.ofMinutes(15);
}
