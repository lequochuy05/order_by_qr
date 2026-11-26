package com.qros.modules.payment.service;

import com.qros.modules.order.service.OrderPaymentReconciliationPort;
import com.qros.modules.payment.model.enums.PaymentTransactionStatus;
import com.qros.modules.payment.repository.PaymentTransactionRepository;
import com.qros.shared.enums.PaymentMethod;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderPaymentReconciliationPortImpl implements OrderPaymentReconciliationPort {

    private static final List<PaymentTransactionStatus> IN_PROGRESS_STATUSES =
            PaymentTransactionStatus.IN_PROGRESS_STATUSES;

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentService paymentService;

    @Override
    public boolean syncLatestPendingPayosPayment(@NonNull Long orderId) {
        var latestTx = paymentTransactionRepository.findFirstByOrderIdAndPaymentMethodAndStatusInOrderByCreatedAtDesc(
                orderId, PaymentMethod.PAYOS, IN_PROGRESS_STATUSES);
        if (latestTx.isPresent()) {
            paymentService.syncPaymentStatus(latestTx.get().getId());
            return true;
        }
        return false;
    }
}
