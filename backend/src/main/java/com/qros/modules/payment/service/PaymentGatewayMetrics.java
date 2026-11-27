package com.qros.modules.payment.service;

import com.qros.shared.enums.PaymentMethod;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.Objects;
import java.util.function.Supplier;

final class PaymentGatewayMetrics {

    private PaymentGatewayMetrics() {}

    static <T> T recordGatewayCall(
            MeterRegistry meterRegistry, String operation, PaymentMethod method, Supplier<T> action) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            return action.get();
        } finally {
            sample.stop(Timer.builder("payment.gateway.latency")
                    .tag("operation", operation)
                    .tag("method", Objects.toString(method, "unknown"))
                    .register(meterRegistry));
        }
    }
}
