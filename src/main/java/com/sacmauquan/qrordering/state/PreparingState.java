package com.sacmauquan.qrordering.state;

import com.sacmauquan.qrordering.model.Order;
import org.springframework.stereotype.Component;

@Component
public class PreparingState implements OrderState {

    @Override
    public void handleRequest(Order order) {
        if (!"PENDING".equals(order.getStatus())) {
            throw new IllegalStateException("Only PENDING orders can be moved to PREPARING state.");
        }
        order.setStatus(getStatusString());
    }

    @Override
    public String getStatusString() {
        return "PREPARING";
    }
}
