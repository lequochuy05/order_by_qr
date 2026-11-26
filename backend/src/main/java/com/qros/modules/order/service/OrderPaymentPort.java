package com.qros.modules.order.service;

import org.springframework.lang.NonNull;

/**
 * Read-only port for checking pending online payment transactions from the order module.
 */
public interface OrderPaymentPort {

    boolean hasPendingPaymentForOrder(@NonNull Long orderId);

    boolean hasPendingPaymentForTableSession(@NonNull Long sessionId);
}
