package com.sacmauquan.qrordering.state;

import com.sacmauquan.qrordering.model.Order;
import org.springframework.stereotype.Component;

/**
 * CompletedState - Handles the logic for orders that have been successfully finished and paid.
 */
@Component
public class CompletedState implements OrderState {

    /**
     * Transitions the order status to COMPLETED.
     * Orders that are already CANCELLED cannot be marked as COMPLETED.
     * 
     * @param order The order entity
     */
    @Override
    public void handleRequest(Order order) {
        if (order.getStatus() == Order.OrderStatus.CANCELLED) {
            throw new IllegalStateException("Cancelled orders cannot be completed.");
        }
        order.setStatus(getStatus());
    }

    @Override
    public Order.OrderStatus getStatus() {
        return Order.OrderStatus.COMPLETED;
    }
}
