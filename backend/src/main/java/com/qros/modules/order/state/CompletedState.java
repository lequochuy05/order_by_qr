package com.qros.modules.order.state;

import com.qros.modules.order.model.Order;
import com.qros.modules.order.model.enums.OrderStatus;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * CompletedState - Handles the logic for orders that have been successfully
 * finished and paid.
 * COMPLETED is an end state — no transitions out are allowed.
 */
@Component
public class CompletedState implements OrderState {

    private static final Set<OrderStatus> ALLOWED_FROM = Set.of(
            OrderStatus.AWAITING_PAYMENT);

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
    public OrderStatus getStatus() {
        return OrderStatus.COMPLETED;
    }

    @Override
    public Set<OrderStatus> allowedTransitionsFrom() {
        return ALLOWED_FROM;
    }
}
