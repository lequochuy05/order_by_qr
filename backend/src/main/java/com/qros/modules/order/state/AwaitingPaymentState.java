package com.qros.modules.order.state;

import com.qros.modules.order.model.Order;
import com.qros.modules.order.model.enums.OrderItemStatus;
import com.qros.modules.order.model.enums.OrderStatus;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * AwaitingPaymentState - Handles orders where all items are finished and the
 * order
 * is waiting for payment settlement by staff/cashier.
 * This state is auto-promoted when all order items reach FINISHED or CANCELLED.
 */
@Component
public class AwaitingPaymentState implements OrderState {

    private static final Set<OrderStatus> ALLOWED_FROM = Set.of(OrderStatus.PENDING, OrderStatus.SERVING);

    /**
     * Validates that all items are done before transitioning to AWAITING_PAYMENT.
     *
     * @param order The order entity
     * @throws IllegalStateException if any items are still in preparation
     */
    @Override
    public void handleRequest(Order order) {
        boolean anyNotDone = order.getOrderItems().stream()
                .anyMatch(i -> i.getStatus() != OrderItemStatus.FINISHED && i.getStatus() != OrderItemStatus.CANCELLED);
        if (anyNotDone) {
            throw new IllegalStateException("Cannot move to AWAITING_PAYMENT: some items are still in preparation");
        }
        order.setStatus(getStatus());
    }

    @Override
    public OrderStatus getStatus() {
        return OrderStatus.AWAITING_PAYMENT;
    }

    @Override
    public Set<OrderStatus> allowedTransitionsFrom() {
        return ALLOWED_FROM;
    }
}
