package com.sacmauquan.qrordering.state;

import com.sacmauquan.qrordering.model.Order;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * CancelledState - Handles the logic for orders that have been cancelled.
 * CANCELLED is an end state — no transitions out are allowed.
 */
@Component
public class CancelledState implements OrderState {

    private static final Set<Order.OrderStatus> ALLOWED_FROM = Set.of(
            Order.OrderStatus.PENDING,
            Order.OrderStatus.SERVING
    );

    /**
     * Transitions the order status to CANCELLED.
     * Only orders that are not already COMPLETED can be cancelled.
     *
     * @param order The order entity
     */
    @Override
    public void handleRequest(Order order) {
        order.setStatus(getStatus());
    }

    @Override
    public Order.OrderStatus getStatus() {
        return Order.OrderStatus.CANCELLED;
    }

    @Override
    public Set<Order.OrderStatus> allowedTransitionsFrom() {
        return ALLOWED_FROM;
    }
}
