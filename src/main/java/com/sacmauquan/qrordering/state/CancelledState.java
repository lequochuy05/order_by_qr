package com.sacmauquan.qrordering.state;

import com.sacmauquan.qrordering.model.Order;
import org.springframework.stereotype.Component;

/**
 * CancelledState - Handles the logic for orders that have been cancelled.
 */
@Component
public class CancelledState implements OrderState {

    /**
     * Transitions the order status to CANCELLED.
     * Only orders that are not already COMPLETED can be cancelled.
     * 
     * @param order The order entity
     */
    @Override
    public void handleRequest(Order order) {
        if (order.getStatus() == Order.OrderStatus.COMPLETED) {
            throw new IllegalStateException("Completed orders cannot be cancelled.");
        }
        order.setStatus(getStatus());
    }

    @Override
    public Order.OrderStatus getStatus() {
        return Order.OrderStatus.CANCELLED;
    }
}
