package com.qros.modules.promotion.service;

import com.qros.modules.order.model.Order;
import com.qros.modules.promotion.model.OrderDiscount;
import com.qros.modules.promotion.model.Voucher;
import com.qros.modules.promotion.repository.OrderDiscountRepository;
import com.qros.modules.promotion.repository.VoucherRepository;
import com.qros.shared.util.AppTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class OrderDiscountService {
    private final OrderDiscountRepository orderDiscountRepository;
    private final VoucherRepository voucherRepository;

    public void recordVoucherSnapshot(Order order, Voucher voucher, BigDecimal appliedAmount) {
        if (order == null || voucher == null || appliedAmount == null || appliedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        upsert(order, voucher, appliedAmount);
    }

    public void recordVoucherSnapshot(Order order, String code, BigDecimal appliedAmount) {
        if (code == null || code.isBlank()) {
            return;
        }
        voucherRepository.findByCodeIgnoreCase(code.trim().toUpperCase())
                .ifPresent(voucher -> recordVoucherSnapshot(order, voucher, appliedAmount));
    }

    private void upsert(Order order, Voucher voucher, BigDecimal appliedAmount) {
        OrderDiscount snapshot = orderDiscountRepository
                .findFirstByOrderIdAndCodeSnapshotIgnoreCase(order.getId(), voucher.getCode())
                .orElseGet(() -> OrderDiscount.builder()
                        .order(order)
                        .voucher(voucher)
                        .codeSnapshot(voucher.getCode())
                        .build());

        snapshot.setVoucher(voucher);
        snapshot.setDiscountTypeSnapshot(voucher.getType());
        snapshot.setDiscountPercentSnapshot(voucher.getDiscountPercent());
        snapshot.setDiscountAmountSnapshot(voucher.getDiscountAmount());
        snapshot.setAppliedAmount(appliedAmount);
        snapshot.setAppliedAt(AppTime.now());
        orderDiscountRepository.save(snapshot);
    }
}
