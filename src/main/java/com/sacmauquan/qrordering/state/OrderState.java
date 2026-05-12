package com.sacmauquan.qrordering.state;

import com.sacmauquan.qrordering.model.Order;

/**
 * OrderState - Common interface for all order states.
 * Implements the State Pattern to handle transitions between different order statuses.
 */
public interface OrderState {
    /**
     * Processes the logic for transitioning an order into the specific state.
     * 
     * @param order The order entity whose state is being updated
     */
    void handleRequest(Order order);

    /**
     * Retrieves the corresponding OrderStatus enum for the state implementation.
     * 
     * @return The OrderStatus enum value
     */
    Order.OrderStatus getStatus();
}
