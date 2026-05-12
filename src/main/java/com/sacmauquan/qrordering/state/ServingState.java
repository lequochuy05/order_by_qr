package com.sacmauquan.qrordering.state;

import com.sacmauquan.qrordering.model.Order;
import org.springframework.stereotype.Component;

/**
 * ServingState - Handles the logic for orders that are currently being prepared or served.
 */
@Component
public class ServingState implements OrderState {

    /**
     * Transitions the order status to SERVING.
     * Only orders that are currently PENDING can transition to the SERVING state.
     * 
     * @param order The order entity
     */
    @Override
    public void handleRequest(Order order) {
        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new IllegalStateException("Only PENDING orders can transition to SERVING status.");
        }
        order.setStatus(getStatus());
    }

    @Override
    public Order.OrderStatus getStatus() {
        return Order.OrderStatus.SERVING;
    }
}
