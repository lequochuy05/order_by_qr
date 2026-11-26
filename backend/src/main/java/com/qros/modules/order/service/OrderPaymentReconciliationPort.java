package com.qros.modules.order.service;

import org.springframework.lang.NonNull;

/**
 * Port for reconciling pending online payment transactions against external payment gateways.
 */
public interface OrderPaymentReconciliationPort {

    boolean syncLatestPendingPayosPayment(@NonNull Long orderId);
}
