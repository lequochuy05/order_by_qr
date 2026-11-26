package com.qros.modules.payment.service;

import com.qros.modules.order.service.OrderPaymentPort;
import com.qros.modules.payment.model.enums.PaymentTransactionStatus;
import com.qros.modules.payment.repository.PaymentTransactionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderPaymentPortImpl implements OrderPaymentPort {

    private static final List<PaymentTransactionStatus> IN_PROGRESS_STATUSES =
            PaymentTransactionStatus.IN_PROGRESS_STATUSES;

    private final PaymentTransactionRepository paymentTransactionRepository;

    @Override
    public boolean hasPendingPaymentForOrder(@NonNull Long orderId) {
        return paymentTransactionRepository.existsByOrderIdAndStatusIn(orderId, IN_PROGRESS_STATUSES);
    }

    @Override
    public boolean hasPendingPaymentForTableSession(@NonNull Long sessionId) {
        return paymentTransactionRepository.existsByOrderTableSessionIdAndStatusIn(sessionId, IN_PROGRESS_STATUSES);
    }
}
