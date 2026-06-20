package com.qros.modules.analytics.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "analytics")
public class AnalyticsProperties {

    private int summaryReconcileDays = 2;
}
