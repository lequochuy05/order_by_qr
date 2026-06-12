package com.qros.modules.order.state;

import com.qros.modules.order.model.Order;
import com.qros.modules.order.model.enums.OrderStatus;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * PendingState - Handles the initial state of an order upon creation.
 */
@Component
public class PendingState implements OrderState {

    private static final Set<OrderStatus> ALLOWED_FROM = Set.of(); // PENDING is the initial state, not reached via
                                                                   // transition

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
    public OrderStatus getStatus() {
        return OrderStatus.PENDING;
    }

    @Override
    public Set<OrderStatus> allowedTransitionsFrom() {
        return ALLOWED_FROM;
    }
}
