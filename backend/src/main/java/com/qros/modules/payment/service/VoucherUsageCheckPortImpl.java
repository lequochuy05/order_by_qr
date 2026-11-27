package com.qros.modules.payment.service;

import com.qros.modules.order.service.OrderLookupService;
import com.qros.modules.payment.model.enums.PaymentTransactionStatus;
import com.qros.modules.payment.repository.PaymentTransactionRepository;
import com.qros.modules.promotion.service.VoucherUsageCheckPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VoucherUsageCheckPortImpl implements VoucherUsageCheckPort {

    private final OrderLookupService orderLookupService;
    private final PaymentTransactionRepository paymentTransactionRepository;

    @Override
    public boolean isVoucherInActiveUse(String voucherCode) {
        if (voucherCode == null || voucherCode.isBlank()) {
            return false;
        }
        String normalized = voucherCode.trim();
        return orderLookupService.existsActiveOrderWithVoucherCode(normalized)
                || paymentTransactionRepository.existsByVoucherCodeSnapshotIgnoreCaseAndStatusIn(
                        normalized, PaymentTransactionStatus.IN_PROGRESS_STATUSES);
    }
}
