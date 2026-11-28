package com.qros.shared.subscription;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.subscription")
public class SubscriptionProperties {

    private boolean enforcementEnabled = true;
    private BigDecimal monthlyPriceVnd = BigDecimal.valueOf(2_000);
    private BigDecimal yearlyPriceVnd = BigDecimal.valueOf(5_000);
    private int trialDays = 14;
    private int freeMaxTables = 10;
    private int freeMaxUsers = 5;
    private int invoiceExpiresInMinutes = 15;
}
