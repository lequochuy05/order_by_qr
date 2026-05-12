package com.sacmauquan.qrordering.state;

import com.sacmauquan.qrordering.model.Order;
import org.springframework.stereotype.Component;

/**
 * PendingState - Handles the initial state of an order upon creation.
 */
@Component
public class PendingState implements OrderState {

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
}
