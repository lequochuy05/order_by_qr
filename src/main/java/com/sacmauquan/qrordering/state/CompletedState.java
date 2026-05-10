package com.sacmauquan.qrordering.state;

import com.sacmauquan.qrordering.model.Order;
import org.springframework.stereotype.Component;

@Component
public class CompletedState implements OrderState {

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
