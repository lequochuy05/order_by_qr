package com.qros.modules.table.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "table-session")
public class TableSessionProperties {

    private Duration noOrderExpire = Duration.ofMinutes(30);
    private Duration tokenIdleTimeout = Duration.ofHours(2);
    private Duration tokenMaxLifetime = Duration.ofHours(12);
    private Duration heartbeatWriteInterval = Duration.ofSeconds(30);

    @Min(1)
    private int maxActiveTokensPerSession = 8;

    @AssertTrue(message = "Table-session durations must be positive and max lifetime must cover idle timeout")
    public boolean isDurationConfigurationValid() {
        return isPositive(noOrderExpire)
                && isPositive(tokenIdleTimeout)
                && isPositive(tokenMaxLifetime)
                && isPositive(heartbeatWriteInterval)
                && tokenMaxLifetime.compareTo(tokenIdleTimeout) >= 0;
    }

    private boolean isPositive(Duration duration) {
        return duration != null && !duration.isZero() && !duration.isNegative();
    }
}
