package com.qros.modules.payment.mapper;

import com.qros.modules.order.dto.response.OrderResponse;
import com.qros.modules.payment.dto.response.PaymentCreateResponse;
import com.qros.modules.payment.dto.response.PaymentOrderResponse;
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

    public PaymentOrderResponse toOrderResponse(OrderResponse order) {
        if (order == null) {
            return null;
        }

        PaymentOrderResponse.TableSummary table = order.table() == null
                ? null
                : new PaymentOrderResponse.TableSummary(
                        order.table().id(), order.table().tableNumber());

        var items = order.orderItems() == null
                ? java.util.List.<PaymentOrderResponse.Item>of()
                : order.orderItems().stream()
                        .map(item -> new PaymentOrderResponse.Item(
                                item.id(),
                                item.batchId(),
                                item.menuItem() == null
                                        ? null
                                        : new PaymentOrderResponse.MenuItemSummary(
                                                item.menuItem().id(),
                                                item.menuItem().name(),
                                                item.menuItem().category() == null
                                                        ? null
                                                        : new PaymentOrderResponse.CategorySummary(item.menuItem()
                                                                .category()
                                                                .name())),
                                item.combo() == null
                                        ? null
                                        : new PaymentOrderResponse.ComboSummary(
                                                item.combo().id(),
                                                item.combo().name(),
                                                item.combo().price()),
                                item.itemNameSnapshot(),
                                enumName(item.itemType()),
                                item.unitPrice(),
                                item.quantity(),
                                item.lineTotal(),
                                item.notes(),
                                item.prepared(),
                                enumName(item.status()),
                                item.options() == null
                                        ? java.util.List.of()
                                        : item.options().stream()
                                                .map(option -> new PaymentOrderResponse.ItemOption(
                                                        option.valueId(),
                                                        option.optionName(),
                                                        option.optionValueName(),
                                                        option.extraPrice()))
                                                .toList(),
                                item.createdAt(),
                                item.updatedAt()))
                        .toList();

        return new PaymentOrderResponse(
                order.id(),
                enumName(order.status()),
                order.voucherCode(),
                order.subtotalAmount(),
                order.discountAmount(),
                order.finalAmount(),
                order.paidAmount(),
                order.businessDate(),
                enumName(order.orderType()),
                enumName(order.paymentStatus()),
                order.paymentMethod(),
                order.paidByName(),
                order.paymentTime(),
                table,
                items,
                order.createdAt());
    }

    private String enumName(Enum<?> value) {
        return value != null ? value.name() : null;
    }
}
