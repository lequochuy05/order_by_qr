package com.qros.modules.payment.dto.internal;

import com.qros.modules.payment.model.PaymentTransaction;

public record OnlinePaymentPreparation(PaymentTransaction transaction, boolean gatewayCallRequired) {}
