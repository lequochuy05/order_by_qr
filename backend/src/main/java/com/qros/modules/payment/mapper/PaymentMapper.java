package com.qros.modules.payment.mapper;

import com.qros.modules.payment.dto.response.PaymentCreateResponse;
import com.qros.modules.payment.dto.response.PaymentTransactionResponse;
import com.qros.modules.payment.model.PaymentTransaction;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public PaymentCreateResponse toCreateResponse(PaymentTransaction transaction) {
        if (transaction == null) {
            return null;
        }

        var order = transaction.getOrder();

        return new PaymentCreateResponse(
                transaction.getId(),
                order != null ? order.getId() : null,
                transaction.getPaymentMethod(),
                transaction.getStatus(),
                transaction.getCheckoutUrl(),
                transaction.getQrCode(),
                transaction.getCreatedAt(),
                transaction.getExpiresAt(),
                transaction.getAmount(),
                order != null ? order.getSubtotalAmount() : null,
                order != null ? order.getDiscountAmount() : null,
                transaction.getAmount(),
                order != null ? order.getVoucherCode() : null,
                transaction.getIdempotencyKey(),
                transaction.getExternalReference());
    }

    public PaymentTransactionResponse toTransactionResponse(PaymentTransaction transaction) {
        if (transaction == null) {
            return null;
        }

        var order = transaction.getOrder();

        return new PaymentTransactionResponse(
                transaction.getId(),
                order != null ? order.getId() : null,
                transaction.getAmount(),
                transaction.getPaymentMethod(),
                transaction.getStatus(),
                transaction.getCheckoutUrl(),
                transaction.getQrCode(),
                transaction.getExternalReference(),
                transaction.getCreatedAt(),
                transaction.getExpiresAt(),
                transaction.getPaidAt(),
                transaction.getBusinessDate(),
                transaction.getFailureReason());
    }
}
