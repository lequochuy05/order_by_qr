package com.sacmauquan.qrordering.state;

import com.sacmauquan.qrordering.model.Order;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * CompletedState - Handles the logic for orders that have been successfully finished and paid.
 * COMPLETED is an end state — no transitions out are allowed.
 */
@Component
public class CompletedState implements OrderState {

    private static final Set<Order.OrderStatus> ALLOWED_FROM = Set.of(
            Order.OrderStatus.PENDING,
            Order.OrderStatus.SERVING
    );

    /**
     * Transitions the order status to COMPLETED.
     * Orders that are already CANCELLED cannot be marked as COMPLETED.
     *
     * @param order The order entity
     */
    @Override
    public void handleRequest(Order order) {
        order.setStatus(getStatus());
    }

    @Override
    public Order.OrderStatus getStatus() {
        return Order.OrderStatus.COMPLETED;
    }

    @Override
    public Set<Order.OrderStatus> allowedTransitionsFrom() {
        return ALLOWED_FROM;
    }
}
