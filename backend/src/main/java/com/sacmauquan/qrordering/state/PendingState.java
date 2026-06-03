package com.sacmauquan.qrordering.state;

import com.sacmauquan.qrordering.model.Order;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * PendingState - Handles the initial state of an order upon creation.
 */
@Component
public class PendingState implements OrderState {

    private static final Set<Order.OrderStatus> ALLOWED_FROM = Set.of(); // PENDING is the initial state, not reached via transition

    /**
     * Sets the order status to PENDING.
     *
     * @param order The order entity
     */
    @Override
    public void handleRequest(Order order) {
        order.setStatus(getStatus());
    }

    @Override
    public Order.OrderStatus getStatus() {
        return Order.OrderStatus.PENDING;
    }

    @Override
    public Set<Order.OrderStatus> allowedTransitionsFrom() {
        return ALLOWED_FROM;
    }
}
