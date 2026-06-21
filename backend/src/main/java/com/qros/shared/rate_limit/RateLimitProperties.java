package com.qros.shared.rate_limit;

import java.time.Duration;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitProperties {

    private List<RateLimitRule> rules = List.of();

    @Getter
    @Setter
    public static class RateLimitRule {
        private String scope;
        private String path;
        private String method;
        private int maxRequests;
        private Duration window = Duration.ofMinutes(1);
    }
}
