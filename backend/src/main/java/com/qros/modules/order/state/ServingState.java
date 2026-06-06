package com.qros.modules.order.state;

import com.qros.modules.order.model.Order;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * ServingState - Handles the logic for orders that are currently being prepared or served.
 */
@Component
public class ServingState implements OrderState {

    private static final Set<Order.OrderStatus> ALLOWED_FROM = Set.of(
            Order.OrderStatus.PENDING,
            Order.OrderStatus.AWAITING_PAYMENT
    );

    /**
     * Transitions the order status to SERVING.
     * Only orders that are currently PENDING can transition to the SERVING state.
     *
     * @param order The order entity
     */
    @Override
    public void handleRequest(Order order) {
        order.setStatus(getStatus());
    }

    @Override
    public Order.OrderStatus getStatus() {
        return Order.OrderStatus.SERVING;
    }

    @Override
    public Set<Order.OrderStatus> allowedTransitionsFrom() {
        return ALLOWED_FROM;
    }
}
