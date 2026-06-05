package com.sacmauquan.qrordering.state;

import com.sacmauquan.qrordering.model.Order;
import com.sacmauquan.qrordering.model.OrderItem;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * AwaitingPaymentState - Handles orders where all items are finished and the order
 * is waiting for payment settlement by staff/cashier.
 * This state is auto-promoted when all order items reach FINISHED or CANCELLED.
 */
@Component
public class AwaitingPaymentState implements OrderState {

    private static final Set<Order.OrderStatus> ALLOWED_FROM = Set.of(
            Order.OrderStatus.PENDING,
            Order.OrderStatus.SERVING
    );

    /**
     * Validates that all items are done before transitioning to AWAITING_PAYMENT.
     *
     * @param order The order entity
     * @throws IllegalStateException if any items are still in preparation
     */
    @Override
    public void handleRequest(Order order) {
        boolean anyNotDone = order.getOrderItems().stream()
                .anyMatch(i -> i.getStatus() != OrderItem.OrderItemStatus.FINISHED
                        && i.getStatus() != OrderItem.OrderItemStatus.CANCELLED);
        if (anyNotDone) {
            throw new IllegalStateException("Cannot move to AWAITING_PAYMENT: some items are still in preparation");
        }
        order.setStatus(getStatus());
    }

    @Override
    public Order.OrderStatus getStatus() {
        return Order.OrderStatus.AWAITING_PAYMENT;
    }

    @Override
    public Set<Order.OrderStatus> allowedTransitionsFrom() {
        return ALLOWED_FROM;
    }
}
