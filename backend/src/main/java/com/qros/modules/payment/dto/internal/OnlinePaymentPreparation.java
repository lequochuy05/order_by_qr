package com.qros.modules.payment.dto.internal;

import com.qros.modules.payment.model.PaymentTransaction;
import java.util.List;

public record OnlinePaymentPreparation(
        PaymentTransaction transaction,
        boolean gatewayCallRequired,
        List<PaymentTransaction> cancelledOnlineTransactions) {

    public OnlinePaymentPreparation(PaymentTransaction transaction, boolean gatewayCallRequired) {
        this(transaction, gatewayCallRequired, List.of());
    }
}
