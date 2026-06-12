package com.qros.modules.promotion.service;

import com.qros.modules.order.model.Order;
import com.qros.modules.promotion.dto.internal.DiscountResult;
import com.qros.modules.promotion.mapper.OrderDiscountMapper;
import com.qros.modules.promotion.model.OrderDiscount;
import com.qros.modules.promotion.model.Voucher;
import com.qros.modules.promotion.repository.OrderDiscountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderDiscountService {

    private final OrderDiscountRepository orderDiscountRepository;
    private final OrderDiscountMapper orderDiscountMapper;

    @Transactional(readOnly = true)
    public List<OrderDiscount> findByOrderId(@NonNull Long orderId) {
        return orderDiscountRepository.findByOrderIdOrderByAppliedAtDesc(orderId);
    }

    @Transactional(readOnly = true)
    public boolean existsByOrderIdAndCode(@NonNull Long orderId, @NonNull String code) {
        return orderDiscountRepository.existsByOrderIdAndCodeSnapshotIgnoreCase(orderId, code);
    }

    @Transactional
    public OrderDiscount recordVoucherDiscount(
            @NonNull Order order,
            Voucher voucher,
            @NonNull DiscountResult discountResult) {
        if (voucher == null || discountResult.voucherCode() == null) {
            return null;
        }

        if (discountResult.appliedDiscountAmount() == null
                || discountResult.appliedDiscountAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        return orderDiscountRepository
                .findByOrderIdAndCodeSnapshotIgnoreCase(order.getId(), discountResult.voucherCode())
                .orElseGet(() -> createSnapshot(order, voucher, discountResult));
    }

    private OrderDiscount createSnapshot(Order order, Voucher voucher, DiscountResult discountResult) {
        OrderDiscount orderDiscount = orderDiscountMapper.toEntity(order, voucher, discountResult);
        return orderDiscountRepository.save(orderDiscount);
    }
}