package com.qros.modules.payment.service;

import com.qros.modules.payment.gateway.PaymentGateway;
import com.qros.modules.payment.gateway.PaymentGatewayResolver;
import com.qros.modules.payment.model.PaymentTransaction;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentGatewayCancellationService {

    private final PaymentGatewayResolver gatewayResolver;
    private final MeterRegistry meterRegistry;

    @Async
    public void cancelPendingOnlineGatewayLinks(List<PaymentTransaction> transactions, String reason) {
        if (transactions == null || transactions.isEmpty()) {
            return;
        }

        for (PaymentTransaction transaction : transactions) {
            try {
                PaymentGateway gateway = gatewayResolver.resolve(transaction.getPaymentMethod());
                recordGatewayCall("cancel", transaction, () -> {
                    gateway.cancelPayment(transaction, reason);
                    return null;
                });
            } catch (Exception exception) {
                log.warn(
                        "Could not cancel online payment link {} after cash settlement: {}",
                        transaction.getId(),
                        exception.getMessage());
            }
        }
    }

    private <T> T recordGatewayCall(
            String operation, PaymentTransaction transaction, java.util.function.Supplier<T> action) {
        return PaymentGatewayMetrics.recordGatewayCall(
                meterRegistry, operation, transaction.getPaymentMethod(), action);
    }
}
