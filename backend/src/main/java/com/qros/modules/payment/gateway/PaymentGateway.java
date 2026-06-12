package com.qros.modules.payment.gateway;

import com.qros.modules.payment.dto.internal.PaymentGatewayCreateResult;
import com.qros.modules.payment.dto.internal.PaymentGatewayStatusResult;
import com.qros.modules.payment.model.PaymentTransaction;
import com.qros.shared.enums.PaymentMethod;

public interface PaymentGateway {

    PaymentMethod getMethod();

    PaymentGatewayCreateResult createPaymentLink(PaymentTransaction transaction);

    void cancelPayment(PaymentTransaction transaction, String reason);

    PaymentGatewayStatusResult getPaymentStatus(PaymentTransaction transaction);
}