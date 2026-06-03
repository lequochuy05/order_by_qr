package com.sacmauquan.qrordering.state;

import com.sacmauquan.qrordering.model.Order;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.EnumMap;
import java.util.List;

/**
 * OrderStateFactory - Manages the initialization and retrieval of OrderState implementations.
 * Centralizes the mapping between OrderStatus enums and their corresponding behavior classes.
 */
@Component
public class OrderStateFactory {
    
    private final Map<Order.OrderStatus, OrderState> states = new EnumMap<>(Order.OrderStatus.class);

    /**
     * Initializes the factory by mapping each injected OrderState to its status.
     * 
     * @param orderStates List of all OrderState implementations managed by Spring
     */
    public OrderStateFactory(List<OrderState> orderStates) {
        for (OrderState state : orderStates) {
            states.put(state.getStatus(), state);
        }
    }

    /**
     * Retrieves the State handler object based on the OrderStatus enum.
     * 
     * @param status The target order status
     * @return The corresponding OrderState implementation
     * @throws IllegalArgumentException if no handler is found for the given status
     */
    public OrderState getState(Order.OrderStatus status) {
        OrderState state = states.get(status);
        if (state == null) {
            throw new IllegalArgumentException("State handler not found for status: " + status);
        }
        return state;
    }

    /**
     * Retrieves the State handler object based on a status name string.
     * Useful for processing input from API requests.
     *
     * @param statusName The name of the order status (case-insensitive)
     * @return The corresponding OrderState implementation
     * @throws IllegalArgumentException if the status name is invalid
     */
    public OrderState getState(String statusName) {
        try {
            Order.OrderStatus status = Order.OrderStatus.valueOf(statusName.toUpperCase());
            return getState(status);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid order status: " + statusName);
        }
    }

    /**
     * Validates that the given current status is allowed to transition into the target status.
     *
     * @param current   The order's current status
     * @param target    The desired new status
     * @throws IllegalStateException if the transition is not allowed
     */
    public void validateTransition(Order.OrderStatus current, Order.OrderStatus target) {
        OrderState targetState = getState(target);
        if (!targetState.allowedTransitionsFrom().contains(current)) {
            throw new IllegalStateException(
                    "Cannot transition from " + current + " to " + target + ". "
                            + "Allowed source statuses for " + target + ": " + targetState.allowedTransitionsFrom());
        }
    }
}
