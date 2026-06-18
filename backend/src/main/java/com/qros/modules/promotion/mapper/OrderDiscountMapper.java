package com.qros.modules.promotion.mapper;

import com.qros.modules.order.model.Order;
import com.qros.modules.promotion.dto.internal.DiscountResult;
import com.qros.modules.promotion.model.OrderDiscount;
import com.qros.modules.promotion.model.Voucher;
import com.qros.shared.time.AppTime;
import org.springframework.stereotype.Component;

@Component
public class OrderDiscountMapper {

    public OrderDiscount toEntity(Order order, Voucher voucher, DiscountResult discountResult) {
        return OrderDiscount.builder()
                .order(order)
                .voucher(voucher)
                .codeSnapshot(discountResult.voucherCode())
                .discountTypeSnapshot(discountResult.voucherType())
                .discountAmountSnapshot(discountResult.discountAmountSnapshot())
                .discountPercentSnapshot(discountResult.discountPercentSnapshot())
                .appliedAmount(discountResult.appliedDiscountAmount())
                .appliedAt(AppTime.now())
                .build();
    }
}
