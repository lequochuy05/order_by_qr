package com.qros.modules.order.state;

import com.qros.modules.order.model.Order;
import com.qros.modules.order.model.enums.OrderStatus;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * CancelledState - Handles the logic for orders that have been cancelled.
 * CANCELLED is an end state — no transitions out are allowed.
 */
@Component
public class CancelledState implements OrderState {

    private static final Set<OrderStatus> ALLOWED_FROM =
            Set.of(OrderStatus.PENDING, OrderStatus.SERVING, OrderStatus.AWAITING_PAYMENT);

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
    public OrderStatus getStatus() {
        return OrderStatus.CANCELLED;
    }

    @Override
    public Set<OrderStatus> allowedTransitionsFrom() {
        return ALLOWED_FROM;
    }
}
